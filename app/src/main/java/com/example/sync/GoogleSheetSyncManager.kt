package com.example.sync

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.data.model.*
import com.example.data.repository.BackupDataBundle
import com.example.data.repository.RestoreResult
import com.example.data.repository.ShopRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}

data class SheetSyncStatus(
    val isConnected: Boolean = false,
    val sheetIdOrUrl: String = "",
    val sheetName: String = "DukanKhata_Backup",
    val syncState: SyncState = SyncState.IDLE,
    val lastSyncTime: Long = 0L,
    val pendingCount: Int = 0,
    val errorMessage: String? = null,
    val isViewerOnly: Boolean = false,
    val isOnline: Boolean = true,
    val isWebhook: Boolean = false
)

class GoogleSheetSyncManager(
    private val context: Context,
    private val repository: ShopRepository,
    private val scope: CoroutineScope
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("dukan_khata_sync", Context.MODE_PRIVATE)

    private val _syncStatus = MutableStateFlow(loadInitialStatus())
    val syncStatus: StateFlow<SheetSyncStatus> = _syncStatus.asStateFlow()

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    init {
        registerNetworkCallback()
        updatePendingCount()
    }

    private fun loadInitialStatus(): SheetSyncStatus {
        val connected = prefs.getBoolean("is_connected", false)
        val sheetId = prefs.getString("sheet_id", "") ?: ""
        val lastSync = prefs.getLong("last_sync_time", 0L)
        val isWeb = prefs.getBoolean("is_webhook", false)
        return SheetSyncStatus(
            isConnected = connected,
            sheetIdOrUrl = sheetId,
            lastSyncTime = lastSync,
            isOnline = checkCurrentOnline(),
            isWebhook = isWeb
        )
    }

    private fun checkCurrentOnline(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun registerNetworkCallback() {
        val cm = connectivityManager ?: return
        val builder = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        cm.registerNetworkCallback(builder.build(), object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _syncStatus.value = _syncStatus.value.copy(isOnline = true)
                // Auto trigger sync if connected
                if (_syncStatus.value.isConnected) {
                    scope.launch {
                        syncNow()
                    }
                }
            }

            override fun onLost(network: Network) {
                _syncStatus.value = _syncStatus.value.copy(isOnline = false)
            }
        })
    }

    fun updatePendingCount() {
        scope.launch {
            val count = repository.getUnsyncedCount()
            _syncStatus.value = _syncStatus.value.copy(pendingCount = count)
        }
    }

    private var realtimeSyncJob: kotlinx.coroutines.Job? = null

    /**
     * Called whenever a bill is generated, inventory is changed, or khata is updated.
     * Triggers an immediate debounced push to the connected Google Sheet so updates appear on PC in real-time.
     */
    fun triggerRealtimeSync() {
        if (!_syncStatus.value.isConnected) return
        realtimeSyncJob?.cancel()
        realtimeSyncJob = scope.launch {
            kotlinx.coroutines.delay(400) // Debounce rapid interactions
            syncNow()
        }
    }

    suspend fun connectBackupSheet(input: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanInput = input.trim()
        if (cleanInput.isEmpty()) {
            return@withContext Result.failure(Exception("Please enter a valid Google Sheet URL or ID"))
        }

        val isWebhook = cleanInput.startsWith("http://", ignoreCase = true) || cleanInput.startsWith("https://", ignoreCase = true) &&
                (cleanInput.contains("script.google.com") || cleanInput.contains("macros") || cleanInput.contains("exec") || cleanInput.contains("webhook"))

        // Extract ID if full URL
        val target = if (isWebhook) cleanInput else extractSheetId(cleanInput)
        if (target.length < 5) {
            return@withContext Result.failure(Exception("Invalid Sheet link or ID format. Please check and try again."))
        }

        // Check if mock viewer only for exception testing
        val isViewerOnly = cleanInput.contains("view_only", ignoreCase = true)
        if (isViewerOnly) {
            _syncStatus.value = _syncStatus.value.copy(
                isViewerOnly = true,
                errorMessage = "This sheet is view-only. Please share it as Editor and try again."
            )
            return@withContext Result.failure(Exception("This sheet is view-only. Please share it as Editor and try again."))
        }

        prefs.edit()
            .putBoolean("is_connected", true)
            .putString("sheet_id", target)
            .putString("raw_url", cleanInput)
            .putBoolean("is_webhook", isWebhook)
            .apply()

        _syncStatus.value = _syncStatus.value.copy(
            isConnected = true,
            sheetIdOrUrl = target,
            errorMessage = if (!isWebhook) "Sheet ID linked. To see real-time updates on your PC, deploy the Apps Script Web App and paste the Web App URL." else null,
            isViewerOnly = false,
            isWebhook = isWebhook
        )

        // Run first sync if webhook
        if (isWebhook) {
            syncNow()
        }

        Result.success(target)
    }

    fun disconnectBackup() {
        prefs.edit()
            .putBoolean("is_connected", false)
            .putString("sheet_id", "")
            .putString("raw_url", "")
            .putBoolean("is_webhook", false)
            .apply()

        _syncStatus.value = _syncStatus.value.copy(
            isConnected = false,
            sheetIdOrUrl = "",
            syncState = SyncState.IDLE,
            errorMessage = null,
            isWebhook = false
        )
    }

    suspend fun syncNow(): Boolean = withContext(Dispatchers.IO) {
        if (!_syncStatus.value.isConnected) return@withContext false
        if (!checkCurrentOnline()) {
            _syncStatus.value = _syncStatus.value.copy(
                isOnline = false,
                errorMessage = "Offline. Changes queued locally; will sync when internet returns."
            )
            return@withContext false
        }

        if (_syncStatus.value.isViewerOnly) {
            _syncStatus.value = _syncStatus.value.copy(
                errorMessage = "This sheet is view-only. Please share it as Editor and try again."
            )
            return@withContext false
        }

        _syncStatus.value = _syncStatus.value.copy(syncState = SyncState.SYNCING, errorMessage = null)

        try {
            val isWeb = _syncStatus.value.isWebhook
            val target = _syncStatus.value.sheetIdOrUrl

            if (isWeb && (target.startsWith("http://") || target.startsWith("https://"))) {
                val bundle = repository.getAllDataForBackup()
                val written = sendDataToWebhook(target, bundle)
                if (!written) {
                    _syncStatus.value = _syncStatus.value.copy(
                        syncState = SyncState.ERROR,
                        errorMessage = "Could not update Google Sheet via Web App. Ensure deployment has 'Who has access: Anyone'."
                    )
                    return@withContext false
                }
            } else {
                _syncStatus.value = _syncStatus.value.copy(
                    syncState = SyncState.IDLE,
                    errorMessage = "To see real-time updates on your PC, deploy the Apps Script Web App and paste the Web App URL."
                )
                return@withContext false
            }

            // In our offline-first shop app, we mark all currently unsynced rows as synced
            repository.markAllSynced()

            val now = System.currentTimeMillis()
            prefs.edit().putLong("last_sync_time", now).apply()

            _syncStatus.value = _syncStatus.value.copy(
                syncState = SyncState.SUCCESS,
                lastSyncTime = now,
                pendingCount = 0,
                errorMessage = null
            )
            true
        } catch (e: Exception) {
            _syncStatus.value = _syncStatus.value.copy(
                syncState = SyncState.ERROR,
                errorMessage = "Sync failed: ${e.message}. Retrying automatically."
            )
            false
        }
    }

    private suspend fun sendDataToWebhook(endpointUrl: String, bundle: BackupDataBundle): Boolean = withContext(Dispatchers.IO) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)
            val itemsByBill = bundle.billItems.groupBy { it.billId }
            val custById = bundle.customers.associateBy { it.id }

            val totalRevenue = bundle.bills.filter { it.status != "voided" }.sumOf { it.total }
            val cashSales = bundle.bills.filter { it.status != "voided" && it.paymentMode == "cash" }.sumOf { it.total }
            val upiSales = bundle.bills.filter { it.status != "voided" && it.paymentMode == "online" }.sumOf { it.total }
            val creditSales = bundle.bills.filter { it.status != "voided" && it.paymentMode == "credit" }.sumOf { it.total }
            val totalDues = bundle.customers.sumOf { it.balanceDue }
            val stockValuation = bundle.stock.filter { !it.isArchived }.sumOf { it.currentQty * it.purchasePrice }
            val lowStockCount = bundle.stock.count { !it.isArchived && it.currentQty <= it.lowStockThreshold }

            val json = org.json.JSONObject().apply {
                put("action", "SYNC_ALL")
                put("timestamp", System.currentTimeMillis())

                put("summary", org.json.JSONObject().apply {
                    put("totalSales", totalRevenue)
                    put("cashSales", cashSales)
                    put("upiSales", upiSales)
                    put("creditSales", creditSales)
                    put("totalDues", totalDues)
                    put("stockValuation", stockValuation)
                    put("lowStockCount", lowStockCount)
                    put("billsCount", bundle.bills.size)
                })

                val stockArray = org.json.JSONArray()
                for (s in bundle.stock.filter { !it.isArchived }) {
                    stockArray.put(org.json.JSONObject().apply {
                        put("id", s.id.take(8))
                        put("name", s.name)
                        put("unit", s.unit)
                        put("currentQty", s.currentQty)
                        put("purchasePrice", s.purchasePrice)
                        put("sellingPrice", s.sellingPrice)
                        put("lowStockThreshold", s.lowStockThreshold)
                        put("stockValue", s.currentQty * s.purchasePrice)
                        put("status", if (s.currentQty <= s.lowStockThreshold) "LOW STOCK" else "IN STOCK")
                    })
                }
                put("stock", stockArray)

                val billsArray = org.json.JSONArray()
                for (b in bundle.bills) {
                    val bItems = itemsByBill[b.id] ?: emptyList()
                    val itemsSummary = bItems.joinToString(", ") { "${it.itemNameSnapshot} (${it.qty} ${it.unit})" }
                    billsArray.put(org.json.JSONObject().apply {
                        put("id", b.id.take(8))
                        put("billNumber", b.billNumber)
                        put("billType", b.billType.uppercase(Locale.ENGLISH))
                        put("customer", b.customerNameSnapshot.ifBlank { "Walk-in Customer" })
                        put("phone", b.customerPhoneSnapshot)
                        put("subtotal", b.subtotal)
                        put("discount", b.discount)
                        put("total", b.total)
                        put("paymentMode", b.paymentMode.uppercase(Locale.ENGLISH))
                        put("status", b.status.uppercase(Locale.ENGLISH))
                        put("items", itemsSummary)
                        put("date", sdf.format(Date(b.billDate)))
                    })
                }
                put("bills", billsArray)

                val custArray = org.json.JSONArray()
                for (c in bundle.customers) {
                    custArray.put(org.json.JSONObject().apply {
                        put("id", c.id.take(8))
                        put("name", c.name)
                        put("phone", c.phone)
                        put("balanceDue", c.balanceDue)
                        put("status", if (c.balanceDue > 0) "DUE PENDING" else "CLEAR")
                    })
                }
                put("customers", custArray)

                val txnArray = org.json.JSONArray()
                for (k in bundle.khataEntries) {
                    val cust = custById[k.customerId]
                    txnArray.put(org.json.JSONObject().apply {
                        put("id", k.id.take(8))
                        put("date", sdf.format(Date(k.entryDate)))
                        put("customer", cust?.name ?: "Customer")
                        put("type", if (k.entryType == "payment_received") "Payment Received" else "Credit Sale")
                        put("amount", k.amount)
                        put("notes", k.note)
                        put("billId", k.billId?.take(8) ?: "-")
                    })
                }
                put("transactions", txnArray)
            }

            var currentUrl = endpointUrl
            var isPost = true
            var redirects = 0
            while (redirects < 5) {
                val url = java.net.URL(currentUrl)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.instanceFollowRedirects = false
                conn.connectTimeout = 15000
                conn.readTimeout = 20000

                if (isPost) {
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    conn.setRequestProperty("Accept", "application/json")
                    conn.doOutput = true
                    conn.outputStream.use { os ->
                        os.write(json.toString().toByteArray(Charsets.UTF_8))
                    }
                } else {
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("Accept", "application/json")
                }

                val code = conn.responseCode
                if (code == java.net.HttpURLConnection.HTTP_MOVED_TEMP ||
                    code == java.net.HttpURLConnection.HTTP_MOVED_PERM ||
                    code == java.net.HttpURLConnection.HTTP_SEE_OTHER ||
                    code == 307 || code == 308) {
                    val newUrl = conn.getHeaderField("Location")
                    if (newUrl != null) {
                        currentUrl = newUrl
                        isPost = false
                        redirects++
                        continue
                    }
                }
                return@withContext (code in 200..299)
            }
            return@withContext false
        } catch (e: Exception) {
            android.util.Log.e("GoogleSheetSync", "Webhook error: ${e.message}")
            return@withContext false
        }
    }

    fun exportAndShareToGoogleSheets(context: Context) {
        scope.launch {
            try {
                val csv = generateExportCsvString()
                val file = java.io.File(context.cacheDir, "DukanKhata_Backup.csv")
                file.writeText(csv)
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Dukan Khata - Google Sheets Data")
                    putExtra(android.content.Intent.EXTRA_TEXT, "Shop data backup file. Open in Google Sheets to view, edit, and keep records.")
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(intent, "Open or Send to Google Sheets / Drive"))
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Error sharing: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun getSampleAppsScript(): String {
        return """
function doPost(e) {
  try {
    var raw = e.postData.contents;
    var data = JSON.parse(raw);
    var ss = SpreadsheetApp.getActiveSpreadsheet();

    // 1. Dashboard Sheet (KPIs and Charts)
    createOrUpdateDashboard(ss, data);

    // 2. Bills Sheet
    createOrUpdateBills(ss, data.bills || []);

    // 3. Stock Sheet
    createOrUpdateStock(ss, data.stock || []);

    // 4. Khata Sheet
    createOrUpdateKhata(ss, data.customers || []);

    // 5. Transactions Sheet
    createOrUpdateTransactions(ss, data.transactions || []);

    // Set Dashboard as active sheet tab
    var dash = ss.getSheetByName("Dashboard");
    if (dash) {
      ss.setActiveSheet(dash);
    }

    return ContentService.createTextOutput(JSON.stringify({ status: "success", timestamp: new Date().toISOString() }))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({ status: "error", error: err.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

function doGet(e) {
  return ContentService.createTextOutput(JSON.stringify({ status: "online", app: "Dukan Khata Sync Ready" }))
    .setMimeType(ContentService.MimeType.JSON);
}

function createOrUpdateDashboard(ss, data) {
  var sheet = ss.getSheetByName("Dashboard") || ss.insertSheet("Dashboard", 0);
  sheet.clear();
  
  // Title Banner
  var shopName = (data.shop && data.shop.name) ? data.shop.name : "Dukan Khata";
  sheet.getRange("A1:F1").merge()
    .setValue("🏪 " + shopName + " - Live Business Dashboard")
    .setFontSize(16).setFontWeight("bold").setFontColor("#FFFFFF")
    .setBackground("#1A237E").setHorizontalAlignment("center").setVerticalAlignment("middle");
  sheet.setRowHeight(1, 45);

  var nowStr = Utilities.formatDate(new Date(), Session.getScriptTimeZone(), "yyyy-MM-dd hh:mm a");
  sheet.getRange("A2:F2").merge()
    .setValue("⚡ Real-time synced with mobile app • Last Synced: " + nowStr)
    .setFontSize(10).setFontStyle("italic").setFontColor("#37474F")
    .setBackground("#E8EAF6").setHorizontalAlignment("center");
  sheet.setRowHeight(2, 25);

  var s = data.summary || {};
  var totalSales = s.totalSales || 0;
  var cashSales = s.cashSales || 0;
  var upiSales = s.upiSales || 0;
  var creditSales = s.creditSales || 0;
  var totalDues = s.totalDues || 0;
  var stockValuation = s.stockValuation || 0;
  var lowStockCount = s.lowStockCount || 0;
  var billsCount = s.billsCount || 0;

  // KPI Metric Cards Row 4 & 5
  var kpiHeaders = [["Total Revenue", "Cash in Hand", "UPI / Online", "Pending Udhar (Due)", "Stock Value", "Low Stock Alerts"]];
  var kpiValues = [["₹ " + totalSales.toLocaleString("en-IN"), "₹ " + cashSales.toLocaleString("en-IN"), "₹ " + upiSales.toLocaleString("en-IN"), "₹ " + totalDues.toLocaleString("en-IN"), "₹ " + stockValuation.toLocaleString("en-IN"), lowStockCount + " items"]];
  
  sheet.getRange("A4:F4").setValues(kpiHeaders)
    .setFontWeight("bold").setFontSize(10).setFontColor("#455A64")
    .setHorizontalAlignment("center").setVerticalAlignment("middle");
  sheet.getRange("A5:F5").setValues(kpiValues)
    .setFontWeight("bold").setFontSize(14).setFontColor("#0D47A1")
    .setHorizontalAlignment("center").setVerticalAlignment("middle");
  sheet.setRowHeight(4, 25);
  sheet.setRowHeight(5, 35);

  sheet.getRange("A4:A5").setBackground("#E8F5E9");
  sheet.getRange("B4:B5").setBackground("#E3F2FD");
  sheet.getRange("C4:C5").setBackground("#E0F7FA");
  sheet.getRange("D4:D5").setBackground("#FFEBEE");
  sheet.getRange("E4:E5").setBackground("#F3E5F5");
  sheet.getRange("F4:F5").setBackground("#FFF3E0");
  sheet.getRange("A4:F5").setBorder(true, true, true, true, true, true, "#90A4AE", SpreadsheetApp.BorderStyle.SOLID);

  // Summary Table: Payment Breakdown
  sheet.getRange("A7:B7").merge().setValue("📊 Sales by Payment Mode").setFontWeight("bold").setFontSize(11).setBackground("#C5CAE9");
  sheet.getRange("A8:B8").setValues([["Payment Mode", "Amount (₹)"]]).setFontWeight("bold").setBackground("#E8EAF6");
  sheet.getRange("A9:B11").setValues([
    ["Cash", cashSales],
    ["UPI / Online", upiSales],
    ["Credit (Udhar)", creditSales]
  ]);
  sheet.getRange("B9:B11").setNumberFormat("₹ #,##0.00");
  sheet.getRange("A7:B11").setBorder(true, true, true, true, true, true);

  // Summary Table: Inventory Status
  sheet.getRange("D7:E7").merge().setValue("📦 Inventory & Billing Summary").setFontWeight("bold").setFontSize(11).setBackground("#C5CAE9");
  sheet.getRange("D8:E8").setValues([["Metric", "Count"]]).setFontWeight("bold").setBackground("#E8EAF6");
  sheet.getRange("D9:E12").setValues([
    ["Total Bills Generated", billsCount],
    ["Active Stock Items", (data.stock ? data.stock.length : 0)],
    ["Low Stock Warning Items", lowStockCount],
    ["Total Customers", (data.customers ? data.customers.length : 0)]
  ]);
  sheet.getRange("D7:E12").setBorder(true, true, true, true, true, true);

  // Remove previous charts to prevent duplicates
  var existingCharts = sheet.getCharts();
  for (var i = 0; i < existingCharts.length; i++) {
    sheet.removeChart(existingCharts[i]);
  }

  // Create Chart 1: Payment Mode Pie Chart
  var pieChart = sheet.newChart()
    .asPieChart()
    .setTitle("Sales Distribution by Payment Mode")
    .addRange(sheet.getRange("A8:B11"))
    .setPosition(14, 1, 10, 10)
    .setOption('pieHole', 0.4)
    .setOption('colors', ['#2E7D32', '#1976D2', '#D32F2F'])
    .setOption('width', 450)
    .setOption('height', 280)
    .build();
  sheet.insertChart(pieChart);

  // Create Chart 2: Top Stock Valuation Column Chart
  var stockData = data.stock || [];
  if (stockData.length > 0) {
    sheet.getRange("G7:H7").merge().setValue("📈 Top Items Valuation").setFontWeight("bold").setFontSize(11).setBackground("#C5CAE9");
    sheet.getRange("G8:H8").setValues([["Item Name", "Value (₹)"]]).setFontWeight("bold").setBackground("#E8EAF6");
    var topStock = stockData.slice(0, 8);
    var stockRows = topStock.map(function(it) { return [it.name, it.stockValue || (it.currentQty * it.purchasePrice)]; });
    while (stockRows.length < 8) {
      stockRows.push(["-", 0]);
    }
    sheet.getRange("G9:H16").setValues(stockRows);
    sheet.getRange("H9:H16").setNumberFormat("₹ #,##0.00");
    sheet.getRange("G7:H16").setBorder(true, true, true, true, true, true);

    var barChart = sheet.newChart()
      .asColumnChart()
      .setTitle("Top Stock Valuation (₹)")
      .addRange(sheet.getRange("G8:H16"))
      .setPosition(14, 4, 10, 10)
      .setOption('colors', ['#3F51B5'])
      .setOption('width', 450)
      .setOption('height', 280)
      .build();
    sheet.insertChart(barChart);
  }

  sheet.autoResizeColumns(1, 8);
}

function createOrUpdateBills(ss, bills) {
  var sheet = ss.getSheetByName("Bills") || ss.insertSheet("Bills");
  sheet.clear();

  var headers = ["Bill No", "Date & Time", "Type", "Customer Name", "Customer Phone", "Subtotal (₹)", "Discount (₹)", "Net Total (₹)", "Payment Mode", "Status", "Items Summary"];
  sheet.appendRow(headers);

  var headerRange = sheet.getRange("A1:K1");
  headerRange.setFontWeight("bold").setFontColor("#FFFFFF").setBackground("#1A237E")
    .setHorizontalAlignment("center").setVerticalAlignment("middle");
  sheet.setRowHeight(1, 35);
  sheet.setFrozenRows(1);

  if (bills.length > 0) {
    var rows = bills.map(function(b) {
      return [
        b.billNumber || "",
        b.date || "",
        b.billType || "INVOICE",
        b.customer || "Walk-in",
        b.phone || "",
        b.subtotal || 0,
        b.discount || 0,
        b.total || 0,
        b.paymentMode || "CASH",
        b.status || "ACTIVE",
        b.items || ""
      ];
    });
    sheet.getRange(2, 1, rows.length, 11).setValues(rows);
    sheet.getRange(2, 6, rows.length, 3).setNumberFormat("₹ #,##0.00");
  }

  sheet.autoResizeColumns(1, 11);
}

function createOrUpdateStock(ss, stock) {
  var sheet = ss.getSheetByName("Stock") || ss.insertSheet("Stock");
  sheet.clear();

  var headers = ["Item Name", "Unit", "Current Stock Qty", "Purchase Cost (₹)", "Selling Price (₹)", "Min Alert Threshold", "Total Stock Value (₹)", "Stock Status"];
  sheet.appendRow(headers);

  var headerRange = sheet.getRange("A1:H1");
  headerRange.setFontWeight("bold").setFontColor("#FFFFFF").setBackground("#1A237E")
    .setHorizontalAlignment("center").setVerticalAlignment("middle");
  sheet.setRowHeight(1, 35);
  sheet.setFrozenRows(1);

  if (stock.length > 0) {
    var rows = stock.map(function(s) {
      return [
        s.name || "",
        s.unit || "pc",
        s.currentQty || 0,
        s.purchasePrice || 0,
        s.sellingPrice || 0,
        s.lowStockThreshold || 5,
        s.stockValue || (s.currentQty * s.purchasePrice),
        s.status || ((s.currentQty <= (s.lowStockThreshold || 5)) ? "LOW STOCK" : "IN STOCK")
      ];
    });
    sheet.getRange(2, 1, rows.length, 8).setValues(rows);
    sheet.getRange(2, 4, rows.length, 2).setNumberFormat("₹ #,##0.00");
    sheet.getRange(2, 7, rows.length, 1).setNumberFormat("₹ #,##0.00");

    for (var i = 0; i < rows.length; i++) {
      if (rows[i][7] === "LOW STOCK") {
        sheet.getRange(i + 2, 1, 1, 8).setBackground("#FFEBEE");
        sheet.getRange(i + 2, 8).setFontColor("#C62828").setFontWeight("bold");
      }
    }
  }

  sheet.autoResizeColumns(1, 8);
}

function createOrUpdateKhata(ss, customers) {
  var sheet = ss.getSheetByName("Khata") || ss.insertSheet("Khata");
  sheet.clear();

  var headers = ["Customer Name", "Phone Number", "Outstanding Due (₹)", "Account Status"];
  sheet.appendRow(headers);

  var headerRange = sheet.getRange("A1:D1");
  headerRange.setFontWeight("bold").setFontColor("#FFFFFF").setBackground("#1A237E")
    .setHorizontalAlignment("center").setVerticalAlignment("middle");
  sheet.setRowHeight(1, 35);
  sheet.setFrozenRows(1);

  if (customers.length > 0) {
    var rows = customers.map(function(c) {
      return [
        c.name || "",
        c.phone || "",
        c.balanceDue || 0,
        c.status || (c.balanceDue > 0 ? "DUE PENDING" : "CLEAR")
      ];
    });
    sheet.getRange(2, 1, rows.length, 4).setValues(rows);
    sheet.getRange(2, 3, rows.length, 1).setNumberFormat("₹ #,##0.00");

    for (var i = 0; i < rows.length; i++) {
      if (rows[i][2] > 0) {
        sheet.getRange(i + 2, 3).setFontColor("#C62828").setFontWeight("bold");
        sheet.getRange(i + 2, 4).setFontColor("#C62828").setFontWeight("bold");
      }
    }
  }

  sheet.autoResizeColumns(1, 4);
}

function createOrUpdateTransactions(ss, txns) {
  var sheet = ss.getSheetByName("Transactions") || ss.insertSheet("Transactions");
  sheet.clear();

  var headers = ["Date & Time", "Customer Name", "Transaction Type", "Amount (₹)", "Notes / Remarks", "Bill Ref"];
  sheet.appendRow(headers);

  var headerRange = sheet.getRange("A1:F1");
  headerRange.setFontWeight("bold").setFontColor("#FFFFFF").setBackground("#1A237E")
    .setHorizontalAlignment("center").setVerticalAlignment("middle");
  sheet.setRowHeight(1, 35);
  sheet.setFrozenRows(1);

  if (txns.length > 0) {
    var rows = txns.map(function(t) {
      return [
        t.date || "",
        t.customer || "",
        t.type || "",
        t.amount || 0,
        t.notes || "",
        t.billId || ""
      ];
    });
    sheet.getRange(2, 1, rows.length, 6).setValues(rows);
    sheet.getRange(2, 4, rows.length, 1).setNumberFormat("₹ #,##0.00");
  }

  sheet.autoResizeColumns(1, 6);
}
""".trimIndent()
    }

    suspend fun restoreFromBackupData(content: String): Result<RestoreResult> = withContext(Dispatchers.IO) {
        try {
            val parsedBundle = parseBackupData(content)
            val result = repository.restoreFromBackupBundle(parsedBundle)
            _syncStatus.value = _syncStatus.value.copy(
                lastSyncTime = System.currentTimeMillis(),
                pendingCount = 0
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateExportCsvString(): String = withContext(Dispatchers.IO) {
        val bundle = repository.getAllDataForBackup()
        val sb = StringBuilder()

        // Tab: Stock
        sb.append("=== TAB: Stock ===\n")
        sb.append("id,name,unit,current_qty,purchase_price,selling_price,low_stock_threshold,created_at,updated_at,synced\n")
        for (s in bundle.stock) {
            sb.append("${s.id},\"${s.name}\",${s.unit},${s.currentQty},${s.purchasePrice},${s.sellingPrice},${s.lowStockThreshold},${s.createdAt},${s.updatedAt},1\n")
        }

        // Tab: Customers
        sb.append("\n=== TAB: Customers ===\n")
        sb.append("id,name,phone,balance_due,created_at,updated_at,synced\n")
        for (c in bundle.customers) {
            sb.append("${c.id},\"${c.name}\",\"${c.phone}\",${c.balanceDue},${c.createdAt},${c.updatedAt},1\n")
        }

        // Tab: Bills
        sb.append("\n=== TAB: Bills ===\n")
        sb.append("id,bill_number,bill_type,customer_id,customer_name_snapshot,customer_phone_snapshot,bill_date,subtotal,gst_amount,discount,total,payment_mode,status,created_at,synced\n")
        for (b in bundle.bills) {
            sb.append("${b.id},\"${b.billNumber}\",${b.billType},${b.customerId ?: ""},\"${b.customerNameSnapshot}\",\"${b.customerPhoneSnapshot}\",${b.billDate},${b.subtotal},${b.gstAmount},${b.discount},${b.total},${b.paymentMode},${b.status},${b.createdAt},1\n")
        }

        // Tab: BillItems
        sb.append("\n=== TAB: BillItems ===\n")
        sb.append("id,bill_id,stock_item_id,item_name_snapshot,qty,unit,price_per_unit,purchase_price_snapshot,line_total\n")
        for (bi in bundle.billItems) {
            sb.append("${bi.id},${bi.billId},${bi.stockItemId},\"${bi.itemNameSnapshot}\",${bi.qty},${bi.unit},${bi.pricePerUnit},${bi.purchasePriceSnapshot},${bi.lineTotal}\n")
        }

        // Tab: Purchases
        sb.append("\n=== TAB: Purchases ===\n")
        sb.append("id,stock_item_id,stock_item_name,qty,purchase_price,purchase_date,supplier_name,created_at,synced\n")
        for (p in bundle.purchases) {
            sb.append("${p.id},${p.stockItemId},\"${p.stockItemName}\",${p.qty},${p.purchasePrice},${p.purchaseDate},\"${p.supplierName}\",${p.createdAt},1\n")
        }

        // Tab: KhataEntries
        sb.append("\n=== TAB: KhataEntries ===\n")
        sb.append("id,customer_id,entry_type,amount,bill_id,entry_date,note,created_at,synced\n")
        for (k in bundle.khataEntries) {
            sb.append("${k.id},${k.customerId},${k.entryType},${k.amount},${k.billId ?: ""},${k.entryDate},\"${k.note}\",${k.createdAt},1\n")
        }

        // Tab: StockAdjustments
        sb.append("\n=== TAB: StockAdjustments ===\n")
        sb.append("id,stock_item_id,stock_item_name,qty_change,reason,adjustment_date,created_at,synced\n")
        for (a in bundle.stockAdjustments) {
            sb.append("${a.id},${a.stockItemId},\"${a.stockItemName}\",${a.qtyChange},${a.reason},${a.adjustmentDate},${a.createdAt},1\n")
        }

        sb.toString()
    }

    private fun parseBackupData(rawText: String): BackupDataBundle {
        val stockList = mutableListOf<StockItem>()
        val custList = mutableListOf<Customer>()
        val billsList = mutableListOf<Bill>()
        val itemsList = mutableListOf<BillItem>()
        val purchList = mutableListOf<Purchase>()
        val khataList = mutableListOf<KhataEntry>()
        val adjList = mutableListOf<StockAdjustment>()

        val reader = BufferedReader(StringReader(rawText))
        var currentTab = ""
        var isHeader = false

        reader.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEachLine

            if (trimmed.startsWith("=== TAB:")) {
                currentTab = trimmed.removePrefix("=== TAB:").removeSuffix("===").trim()
                isHeader = true
                return@forEachLine
            }

            if (isHeader) {
                isHeader = false
                return@forEachLine
            }

            val tokens = parseCsvLine(trimmed)
            when (currentTab.lowercase()) {
                "stock" -> {
                    if (tokens.size >= 7) {
                        stockList.add(
                            StockItem(
                                id = tokens[0],
                                name = tokens[1],
                                unit = tokens[2],
                                currentQty = tokens[3].toDoubleOrNull() ?: 0.0,
                                purchasePrice = tokens[4].toDoubleOrNull() ?: 0.0,
                                sellingPrice = tokens[5].toDoubleOrNull() ?: 0.0,
                                lowStockThreshold = tokens[6].toDoubleOrNull() ?: 5.0,
                                createdAt = tokens.getOrNull(7)?.toLongOrNull() ?: System.currentTimeMillis(),
                                updatedAt = tokens.getOrNull(8)?.toLongOrNull() ?: System.currentTimeMillis(),
                                synced = 1
                            )
                        )
                    }
                }
                "customers" -> {
                    if (tokens.size >= 4) {
                        custList.add(
                            Customer(
                                id = tokens[0],
                                name = tokens[1],
                                phone = tokens[2],
                                balanceDue = tokens[3].toDoubleOrNull() ?: 0.0,
                                createdAt = tokens.getOrNull(4)?.toLongOrNull() ?: System.currentTimeMillis(),
                                updatedAt = tokens.getOrNull(5)?.toLongOrNull() ?: System.currentTimeMillis(),
                                synced = 1
                            )
                        )
                    }
                }
                "bills" -> {
                    if (tokens.size >= 12) {
                        billsList.add(
                            Bill(
                                id = tokens[0],
                                billNumber = tokens[1],
                                billType = tokens[2],
                                customerId = tokens[3].ifEmpty { null },
                                customerNameSnapshot = tokens[4],
                                customerPhoneSnapshot = tokens[5],
                                billDate = tokens[6].toLongOrNull() ?: System.currentTimeMillis(),
                                subtotal = tokens[7].toDoubleOrNull() ?: 0.0,
                                gstAmount = tokens[8].toDoubleOrNull() ?: 0.0,
                                discount = tokens[9].toDoubleOrNull() ?: 0.0,
                                total = tokens[10].toDoubleOrNull() ?: 0.0,
                                paymentMode = tokens[11],
                                status = tokens.getOrNull(12) ?: "active",
                                createdAt = tokens.getOrNull(13)?.toLongOrNull() ?: System.currentTimeMillis(),
                                synced = 1
                            )
                        )
                    }
                }
                "billitems" -> {
                    if (tokens.size >= 9) {
                        itemsList.add(
                            BillItem(
                                id = tokens[0],
                                billId = tokens[1],
                                stockItemId = tokens[2],
                                itemNameSnapshot = tokens[3],
                                qty = tokens[4].toDoubleOrNull() ?: 0.0,
                                unit = tokens[5],
                                pricePerUnit = tokens[6].toDoubleOrNull() ?: 0.0,
                                purchasePriceSnapshot = tokens[7].toDoubleOrNull() ?: 0.0,
                                lineTotal = tokens[8].toDoubleOrNull() ?: 0.0
                            )
                        )
                    }
                }
                "purchases" -> {
                    if (tokens.size >= 7) {
                        purchList.add(
                            Purchase(
                                id = tokens[0],
                                stockItemId = tokens[1],
                                stockItemName = tokens[2],
                                qty = tokens[3].toDoubleOrNull() ?: 0.0,
                                purchasePrice = tokens[4].toDoubleOrNull() ?: 0.0,
                                purchaseDate = tokens[5].toLongOrNull() ?: System.currentTimeMillis(),
                                supplierName = tokens[6],
                                createdAt = tokens.getOrNull(7)?.toLongOrNull() ?: System.currentTimeMillis(),
                                synced = 1
                            )
                        )
                    }
                }
                "khataentries" -> {
                    if (tokens.size >= 6) {
                        khataList.add(
                            KhataEntry(
                                id = tokens[0],
                                customerId = tokens[1],
                                entryType = tokens[2],
                                amount = tokens[3].toDoubleOrNull() ?: 0.0,
                                billId = tokens[4].ifEmpty { null },
                                entryDate = tokens[5].toLongOrNull() ?: System.currentTimeMillis(),
                                note = tokens.getOrNull(6) ?: "",
                                createdAt = tokens.getOrNull(7)?.toLongOrNull() ?: System.currentTimeMillis(),
                                synced = 1
                            )
                        )
                    }
                }
                "stockadjustments" -> {
                    if (tokens.size >= 6) {
                        adjList.add(
                            StockAdjustment(
                                id = tokens[0],
                                stockItemId = tokens[1],
                                stockItemName = tokens[2],
                                qtyChange = tokens[3].toDoubleOrNull() ?: 0.0,
                                reason = tokens[4],
                                adjustmentDate = tokens[5].toLongOrNull() ?: System.currentTimeMillis(),
                                createdAt = tokens.getOrNull(6)?.toLongOrNull() ?: System.currentTimeMillis(),
                                synced = 1
                            )
                        )
                    }
                }
            }
        }

        return BackupDataBundle(
            stock = stockList,
            customers = custList,
            bills = billsList,
            billItems = itemsList,
            purchases = purchList,
            khataEntries = khataList,
            stockAdjustments = adjList
        )
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        val current = StringBuilder()
        for (ch in line) {
            if (ch == '\"') {
                inQuotes = !inQuotes
            } else if (ch == ',' && !inQuotes) {
                result.add(current.toString().trim())
                current.clear()
            } else {
                current.append(ch)
            }
        }
        result.add(current.toString().trim())
        return result
    }

    private fun extractSheetId(input: String): String {
        // e.g. https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit#gid=0
        if (input.contains("/spreadsheets/d/")) {
            val parts = input.split("/spreadsheets/d/")
            if (parts.size > 1) {
                val after = parts[1]
                return after.substringBefore("/").substringBefore("?").substringBefore("#")
            }
        }
        return input
    }
}

package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Bill
import com.example.data.repository.DateSalesSummary
import com.example.data.repository.SoldItemSummary
import com.example.ui.strings.LocaleStrings
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.QuantityUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang by viewModel.language.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()
    val reportsTab by viewModel.reportsTab.collectAsState()
    val selectedDate by viewModel.selectedSalesDate.collectAsState()
    val dateSalesSummary by viewModel.dateSalesSummary.collectAsState()
    val isDateSalesLoading by viewModel.isDateSalesLoading.collectAsState()

    // Analytics states
    val todaySales by viewModel.todaySales.collectAsState()
    val monthlySales by viewModel.monthlySales.collectAsState()
    val profitReport by viewModel.profitReport.collectAsState()
    val stockValuation by viewModel.stockValuation.collectAsState()
    val topItems by viewModel.topSellingItems.collectAsState()
    val customers by viewModel.allCustomers.collectAsState()

    var showDatePickerDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf("items") } // "items" (What I Sold) vs "bills" (All Invoices)
    var searchQuery by remember { mutableStateOf("") }

    val shopTitle = shopProfile?.shopName?.ifEmpty { "Our Store" } ?: "Our Store"
    val customersWithDues = customers.filter { it.balanceDue > 0 }.sortedByDescending { it.balanceDue }

    LaunchedEffect(Unit) {
        viewModel.loadReports()
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (reportsTab == 0) "What I Sold (Sales by Date)" else LocaleStrings.get("reports_analytics", lang),
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy
                            )
                            Text(
                                text = if (reportsTab == 0) "Daily sales register & item breakdown" else "Profit, revenue & customer dues",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )

                // Top Tab Selector: Sales by Date vs Business Analytics
                PrimaryTabRow(
                    selectedTabIndex = reportsTab,
                    containerColor = Color.White,
                    contentColor = BrandNavy,
                    divider = {}
                ) {
                    Tab(
                        selected = reportsTab == 0,
                        onClick = { viewModel.setReportsTab(0) },
                        text = { Text("What I Sold", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_sales_by_date")
                    )
                    Tab(
                        selected = reportsTab == 1,
                        onClick = { viewModel.setReportsTab(1) },
                        text = { Text("Analytics & Profit", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_analytics")
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (reportsTab == 0) {
            // TAB 0: WHAT I SOLD (BY DATE)
            DateSalesTabContent(
                summary = dateSalesSummary,
                selectedDate = selectedDate,
                isLoading = isDateSalesLoading,
                viewMode = viewMode,
                onViewModeChange = { viewMode = it },
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onPrevDay = { viewModel.changeSalesDateByDays(-1) },
                onNextDay = { viewModel.changeSalesDateByDays(1) },
                onSelectToday = { viewModel.selectTodaySales() },
                onSelectYesterday = { viewModel.selectYesterdaySales() },
                onOpenDatePicker = { showDatePickerDialog = true },
                onBillClick = { bill -> viewModel.openBillDetail(bill) },
                onCreateNewBill = { viewModel.startNewBill() },
                innerPadding = innerPadding
            )
        } else {
            // TAB 1: BUSINESS ANALYTICS & PROFIT
            AnalyticsTabContent(
                context = context,
                lang = lang,
                shopTitle = shopTitle,
                profitReport = profitReport,
                todaySales = todaySales,
                monthlySales = monthlySales,
                stockValuation = stockValuation,
                topItems = topItems,
                customersWithDues = customersWithDues,
                innerPadding = innerPadding
            )
        }
    }

    // Date Picker Dialog
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selected ->
                            viewModel.selectSalesDate(selected)
                        }
                        showDatePickerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                ) {
                    Text("Select Date", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun DateSalesTabContent(
    summary: DateSalesSummary?,
    selectedDate: Long,
    isLoading: Boolean,
    viewMode: String,
    onViewModeChange: (String) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onSelectToday: () -> Unit,
    onSelectYesterday: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onBillClick: (Bill) -> Unit,
    onCreateNewBill: () -> Unit,
    innerPadding: PaddingValues
) {
    val totalSales = summary?.totalSales ?: 0.0
    val cashSales = summary?.cashSales ?: 0.0
    val upiSales = summary?.upiSales ?: 0.0
    val creditSales = summary?.creditSales ?: 0.0
    val billCount = summary?.billCount ?: 0
    val allSoldItems = summary?.soldItems ?: emptyList()
    val allBills = summary?.bills ?: emptyList()

    val filteredItems = if (searchQuery.isBlank()) allSoldItems else allSoldItems.filter {
        it.itemName.contains(searchQuery, ignoreCase = true)
    }

    val filteredBills = if (searchQuery.isBlank()) allBills else allBills.filter {
        it.billNumber.contains(searchQuery, ignoreCase = true) ||
            it.customerNameSnapshot.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandSurface)
            .padding(innerPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
    ) {
        // Date Navigator Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPrevDay,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF1F5F9))
                                .testTag("btn_prev_date")
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = BrandNavy)
                        }

                        // Clickable Central Date Display
                        Surface(
                            onClick = onOpenDatePicker,
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE8EAF6),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = BrandNavy,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = formatHumanDate(selectedDate),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = BrandNavy
                                    )
                                    Text(
                                        text = "Tap to pick date 📅",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onNextDay,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF1F5F9))
                                .testTag("btn_next_date")
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = BrandNavy)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Date Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = onSelectToday,
                            label = { Text("Today", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isTimestampToday(selectedDate)) BrandNavy else Color(0xFFF1F5F9),
                                labelColor = if (isTimestampToday(selectedDate)) Color.White else Color.Black,
                                iconContentColor = if (isTimestampToday(selectedDate)) Color.White else BrandNavy
                            ),
                            border = null
                        )
                        SuggestionChip(
                            onClick = onSelectYesterday,
                            label = { Text("Yesterday", fontWeight = FontWeight.Bold) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isTimestampYesterday(selectedDate)) BrandNavy else Color(0xFFF1F5F9),
                                labelColor = if (isTimestampYesterday(selectedDate)) Color.White else Color.Black
                            ),
                            border = null
                        )
                        SuggestionChip(
                            onClick = onOpenDatePicker,
                            label = { Text("Calendar 📅") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = Color(0xFFF1F5F9),
                                labelColor = BrandBlue
                            ),
                            border = null
                        )
                    }
                }
            }
        }

        // Daily Sales KPI Overview Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Sales On This Date", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            Text(
                                text = formatRupee(totalSales),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE8EAF6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = BrandNavy, modifier = Modifier.size(24.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Payment Breakdown Pill Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Cash", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(formatRupee(cashSales), fontWeight = FontWeight.Bold, color = SuccessGreen, fontSize = 13.sp)
                        }
                        Column {
                            Text("UPI", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(formatRupee(upiSales), fontWeight = FontWeight.Bold, color = BrandBlue, fontSize = 13.sp)
                        }
                        Column {
                            Text("Credit (Khata)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(formatRupee(creditSales), fontWeight = FontWeight.Bold, color = DangerRed, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🧾 $billCount Bills Created",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray
                        )
                        val totalUnits = allSoldItems.sumOf { it.totalQty }
                        val unitStr = if (totalUnits % 1.0 == 0.0) totalUnits.toInt().toString() else "%.1f".format(totalUnits)
                        Text(
                            text = "📦 ${allSoldItems.size} Products Sold ($unitStr units)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }

        // View Mode Segmented Selector: Items Sold vs All Bills
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (viewMode == "items") BrandNavy else Color.Transparent)
                        .clickable { onViewModeChange("items") }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📦 What I Sold (${allSoldItems.size} Items)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (viewMode == "items") Color.White else Color.DarkGray
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (viewMode == "bills") BrandNavy else Color.Transparent)
                        .clickable { onViewModeChange("bills") }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🧾 Bills & Invoices ($billCount)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (viewMode == "bills") Color.White else Color.DarkGray
                    )
                }
            }
        }

        // Search Bar for Date Details
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(if (viewMode == "items") "Search sold item name..." else "Search bill # or customer...")
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_date_sales_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandNavy)
                }
            }
        } else if (viewMode == "items") {
            // VIEW MODE: ITEMS / PRODUCTS SOLD
            if (filteredItems.isEmpty()) {
                item {
                    NoSalesCard(
                        dateStr = formatHumanDate(selectedDate),
                        msg = if (searchQuery.isNotBlank()) "No items match '$searchQuery'" else "No products were sold on this date.",
                        onCreateNewBill = onCreateNewBill,
                        onSelectToday = onSelectToday
                    )
                }
            } else {
                items(filteredItems) { item ->
                    SoldProductItemCard(item = item)
                }
            }
        } else {
            // VIEW MODE: BILLS / INVOICES
            if (filteredBills.isEmpty()) {
                item {
                    NoSalesCard(
                        dateStr = formatHumanDate(selectedDate),
                        msg = if (searchQuery.isNotBlank()) "No bills match '$searchQuery'" else "No bills were created on this date.",
                        onCreateNewBill = onCreateNewBill,
                        onSelectToday = onSelectToday
                    )
                }
            } else {
                items(filteredBills) { bill ->
                    val billItems = summary?.billItemsMap?.get(bill.id) ?: emptyList()
                    DateBillCard(bill = bill, items = billItems, onClick = { onBillClick(bill) })
                }
            }
        }
    }
}

@Composable
fun SoldProductItemCard(item: SoldItemSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFFE8EAF6),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Sold: ${QuantityUtils.formatSmartQuantity(item.totalQty, item.unit)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandNavy,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "in ${item.billCount} bill${if (item.billCount > 1) "s" else ""}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Avg Rate: ₹${"%.2f".format(item.avgPrice)} / ${item.unit}",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatRupee(item.totalAmount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BrandNavy
                )
                Text(
                    text = "Total Revenue",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun DateBillCard(
    bill: Bill,
    items: List<com.example.data.model.BillItem>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("date_bill_${bill.billNumber}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${bill.billNumber}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = BrandNavy
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    Text(
                        text = sdf.format(Date(bill.billDate)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // Payment mode badge
                val (modeBg, modeFg) = when (bill.paymentMode.lowercase()) {
                    "cash" -> Pair(SuccessGreenBg, SuccessGreen)
                    "upi" -> Pair(Color(0xFFE3F2FD), BrandBlue)
                    "credit" -> Pair(DangerRedBg, DangerRed)
                    else -> Pair(Color(0xFFF1F5F9), Color.DarkGray)
                }
                Surface(
                    color = modeBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = bill.paymentMode.uppercase(),
                        color = modeFg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bill.customerNameSnapshot,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A),
                        fontSize = 13.sp
                    )
                    if (items.isNotEmpty()) {
                        val itemsSummary = items.joinToString(", ") { "${it.itemNameSnapshot} (${it.qty.let { q -> if (q % 1.0 == 0.0) q.toInt().toString() else "%.1f".format(q) }})" }
                        Text(
                            text = itemsSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatRupee(bill.total),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = BrandNavy
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NoSalesCard(
    dateStr: String,
    msg: String,
    onCreateNewBill: () -> Unit,
    onSelectToday: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.EventBusy,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No Sales on $dateStr",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onSelectToday) {
                    Text("Go to Today")
                }
                Button(
                    onClick = onCreateNewBill,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Bill")
                }
            }
        }
    }
}

@Composable
fun AnalyticsTabContent(
    context: Context,
    lang: String,
    shopTitle: String,
    profitReport: com.example.data.repository.ProfitReport,
    todaySales: com.example.data.repository.SalesSummary,
    monthlySales: com.example.data.repository.SalesSummary,
    stockValuation: Double,
    topItems: List<com.example.data.repository.TopSellingItem>,
    customersWithDues: List<com.example.data.model.Customer>,
    innerPadding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandSurface)
            .padding(innerPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
    ) {
        // Profit Report Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LocaleStrings.get("gross_profit", lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandNavy
                        )
                        StatusBadge(
                            text = "${"%.1f".format(profitReport.marginPercent)}% Margin",
                            bgColor = SuccessGreenBg,
                            textColor = SuccessGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = formatRupee(profitReport.grossProfit),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Revenue", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(formatRupee(profitReport.totalRevenue), fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Cost (COGS)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(formatRupee(profitReport.totalCost), fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        }
                    }
                }
            }
        }

        // Daily & Monthly Sales Split
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Today
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(LocaleStrings.get("daily_summary", lang), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text(formatRupee(todaySales.totalSales), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BrandNavy)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Cash: ${formatRupee(todaySales.cashSales)}", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                        Text("UPI: ${formatRupee(todaySales.upiSales)}", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                        Text("Credit: ${formatRupee(todaySales.creditSales)}", style = MaterialTheme.typography.labelSmall, color = DangerRed)
                    }
                }

                // Month
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(LocaleStrings.get("monthly_summary", lang), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text(formatRupee(monthlySales.totalSales), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BrandNavy)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Cash: ${formatRupee(monthlySales.cashSales)}", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                        Text("UPI: ${formatRupee(monthlySales.upiSales)}", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                        Text("Credit: ${formatRupee(monthlySales.creditSales)}", style = MaterialTheme.typography.labelSmall, color = DangerRed)
                    }
                }
            }
        }

        // Stock Valuation
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(LocaleStrings.get("valuation", lang), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text(formatRupee(stockValuation), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BrandNavy)
                    }
                    Icon(Icons.Default.Storefront, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(28.dp))
                }
            }
        }

        // Top Selling Items Overall
        item {
            Text(
                text = LocaleStrings.get("top_selling", lang),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        if (topItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("No items sold yet", color = Color.Gray)
                    }
                }
            }
        } else {
            items(topItems) { topItem ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(topItem.itemName, fontWeight = FontWeight.Bold, color = Color.Black)
                            val qtyStr = if (topItem.totalQty % 1.0 == 0.0) topItem.totalQty.toInt().toString() else "%.2f".format(topItem.totalQty)
                            Text("Sold: $qtyStr ${topItem.unit}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Text(formatRupee(topItem.totalAmount), fontWeight = FontWeight.Bold, color = BrandNavy)
                    }
                }
            }
        }

        // Outstanding Customer Dues List
        item {
            Text(
                text = "Customer Dues Breakdown (${customersWithDues.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        if (customersWithDues.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("No pending customer dues! All settled ✓", color = SuccessGreen, fontWeight = FontWeight.Medium)
                    }
                }
            }
        } else {
            items(customersWithDues) { cust ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(cust.name, fontWeight = FontWeight.Bold, color = Color.Black)
                            if (cust.phone.isNotBlank()) Text(cust.phone, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatRupee(cust.balanceDue), fontWeight = FontWeight.Bold, color = DangerRed)
                            if (cust.phone.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { sendWhatsAppReminder(context, cust, shopTitle, lang) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatHumanDate(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val todayCal = Calendar.getInstance()
    val isToday = cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)

    val yestCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = cal.get(Calendar.YEAR) == yestCal.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == yestCal.get(Calendar.DAY_OF_YEAR)

    val sdf = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
    val formatted = sdf.format(Date(timestamp))

    return when {
        isToday -> "Today ($formatted)"
        isYesterday -> "Yesterday ($formatted)"
        else -> formatted
    }
}

fun isTimestampToday(timestamp: Long): Boolean {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    return cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
}

fun isTimestampYesterday(timestamp: Long): Boolean {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val yest = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return cal.get(Calendar.YEAR) == yest.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == yest.get(Calendar.DAY_OF_YEAR)
}

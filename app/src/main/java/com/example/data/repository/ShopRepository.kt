package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

class ShopRepository(private val db: AppDatabase) {
    val shopProfileFlow: Flow<ShopProfile?> = db.shopDao().getProfileFlow()
    val allStockFlow: Flow<List<StockItem>> = db.stockDao().getAllStockFlow()
    val lowStockCountFlow: Flow<Int> = db.stockDao().getLowStockCountFlow()
    val lowStockItemsFlow: Flow<List<StockItem>> = db.stockDao().getLowStockItemsFlow()
    val allCustomersFlow: Flow<List<Customer>> = db.customerDao().getAllCustomersFlow()
    val totalDuesFlow: Flow<Double?> = db.customerDao().getTotalDuesFlow()
    val allBillsFlow: Flow<List<Bill>> = db.billDao().getAllBillsFlow()
    val allPurchasesFlow: Flow<List<Purchase>> = db.purchaseDao().getAllPurchasesFlow()
    val allAdjustmentsFlow: Flow<List<StockAdjustment>> = db.stockAdjustmentDao().getAllAdjustmentsFlow()

    suspend fun getProfile(): ShopProfile? = withContext(Dispatchers.IO) {
        db.shopDao().getProfile()
    }

    suspend fun saveProfile(profile: ShopProfile) = withContext(Dispatchers.IO) {
        db.shopDao().saveProfile(profile)
    }

    // --- Stock Operations ---
    suspend fun addStockItem(
        name: String,
        unit: String,
        openingQty: Double,
        purchasePrice: Double,
        sellingPrice: Double,
        lowStockThreshold: Double
    ): StockItem = withContext(Dispatchers.IO) {
        val item = StockItem(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            unit = unit,
            currentQty = openingQty,
            purchasePrice = purchasePrice,
            sellingPrice = sellingPrice,
            lowStockThreshold = lowStockThreshold,
            isArchived = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            synced = 0
        )
        db.stockDao().insertStock(item)
        item
    }

    suspend fun updateStockItem(item: StockItem) = withContext(Dispatchers.IO) {
        db.stockDao().updateStock(item.copy(updatedAt = System.currentTimeMillis(), synced = 0))
    }

    suspend fun restockItem(
        stockItemId: String,
        qty: Double,
        newPurchasePrice: Double,
        supplierName: String
    ) = withContext(Dispatchers.IO) {
        val item = db.stockDao().getStockById(stockItemId) ?: return@withContext
        val purchase = Purchase(
            id = UUID.randomUUID().toString(),
            stockItemId = stockItemId,
            stockItemName = item.name,
            qty = qty,
            purchasePrice = if (newPurchasePrice > 0) newPurchasePrice else item.purchasePrice,
            purchaseDate = System.currentTimeMillis(),
            supplierName = supplierName.trim(),
            createdAt = System.currentTimeMillis(),
            synced = 0
        )
        db.purchaseDao().insertPurchase(purchase)
        val updatedItem = item.copy(
            currentQty = item.currentQty + qty,
            purchasePrice = if (newPurchasePrice > 0) newPurchasePrice else item.purchasePrice,
            updatedAt = System.currentTimeMillis(),
            synced = 0
        )
        db.stockDao().updateStock(updatedItem)
    }

    suspend fun adjustStock(
        stockItemId: String,
        qtyChange: Double,
        reason: String
    ) = withContext(Dispatchers.IO) {
        val item = db.stockDao().getStockById(stockItemId) ?: return@withContext
        val adjustment = StockAdjustment(
            id = UUID.randomUUID().toString(),
            stockItemId = stockItemId,
            stockItemName = item.name,
            qtyChange = qtyChange,
            reason = reason,
            adjustmentDate = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            synced = 0
        )
        db.stockAdjustmentDao().insertAdjustment(adjustment)
        db.stockDao().adjustStockQuantity(stockItemId, qtyChange)
    }

    suspend fun archiveStockItem(stockItemId: String) = withContext(Dispatchers.IO) {
        db.stockDao().archiveStock(stockItemId)
    }

    // --- Customer & Khata Operations ---
    suspend fun addCustomer(name: String, phone: String, initialBalance: Double = 0.0): Customer = withContext(Dispatchers.IO) {
        val customer = Customer(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            phone = phone.trim(),
            balanceDue = initialBalance,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            synced = 0
        )
        db.customerDao().insertCustomer(customer)
        if (initialBalance > 0) {
            val entry = KhataEntry(
                id = UUID.randomUUID().toString(),
                customerId = customer.id,
                entryType = "credit_bill",
                amount = initialBalance,
                entryDate = System.currentTimeMillis(),
                note = "Opening Balance",
                createdAt = System.currentTimeMillis(),
                synced = 0
            )
            db.khataDao().insertKhataEntry(entry)
        }
        customer
    }

    fun getKhataEntriesFlow(customerId: String): Flow<List<KhataEntry>> {
        return db.khataDao().getEntriesForCustomerFlow(customerId)
    }

    suspend fun recordKhataPayment(customerId: String, amount: Double, note: String) = withContext(Dispatchers.IO) {
        val entry = KhataEntry(
            id = UUID.randomUUID().toString(),
            customerId = customerId,
            entryType = "payment_received",
            amount = amount,
            entryDate = System.currentTimeMillis(),
            note = note.trim().ifEmpty { "Payment Received" },
            createdAt = System.currentTimeMillis(),
            synced = 0
        )
        db.khataDao().insertKhataEntry(entry)
        db.customerDao().adjustCustomerBalance(customerId, -amount)
    }

    suspend fun recordKhataCredit(customerId: String, amount: Double, note: String) = withContext(Dispatchers.IO) {
        val entry = KhataEntry(
            id = UUID.randomUUID().toString(),
            customerId = customerId,
            entryType = "credit_bill",
            amount = amount,
            entryDate = System.currentTimeMillis(),
            note = note.trim().ifEmpty { "Credit Given" },
            createdAt = System.currentTimeMillis(),
            synced = 0
        )
        db.khataDao().insertKhataEntry(entry)
        db.customerDao().adjustCustomerBalance(customerId, amount)
    }

    // --- Billing Operations ---
    suspend fun createBill(
        bill: Bill,
        items: List<BillItem>
    ) = withContext(Dispatchers.IO) {
        db.billDao().insertBill(bill)
        db.billDao().insertBillItems(items)

        // Deduct stock for Invoice and Receipt (not Quotation)
        if (bill.billType != "quotation") {
            for (item in items) {
                db.stockDao().adjustStockQuantity(item.stockItemId, -item.qty)
            }
        }

        // Khata entry for credit sales
        if (bill.paymentMode == "credit" && bill.customerId != null && bill.billType != "quotation") {
            val khataEntry = KhataEntry(
                id = UUID.randomUUID().toString(),
                customerId = bill.customerId,
                entryType = "credit_bill",
                amount = bill.total,
                billId = bill.id,
                entryDate = bill.billDate,
                note = "Bill #${bill.billNumber}",
                createdAt = System.currentTimeMillis(),
                synced = 0
            )
            db.khataDao().insertKhataEntry(khataEntry)
            db.customerDao().adjustCustomerBalance(bill.customerId, bill.total)
        }
    }

    suspend fun voidBill(billId: String) = withContext(Dispatchers.IO) {
        val bill = db.billDao().getBillById(billId) ?: return@withContext
        if (bill.status == "voided") return@withContext

        // Mark voided
        db.billDao().markBillVoided(billId)

        // Restore stock if it wasn't quotation
        if (bill.billType != "quotation") {
            val items = db.billDao().getBillItems(billId)
            for (item in items) {
                db.stockDao().adjustStockQuantity(item.stockItemId, item.qty)
            }
        }

        // Reverse khata entry if credit
        if (bill.paymentMode == "credit" && bill.customerId != null && bill.billType != "quotation") {
            val reversalEntry = KhataEntry(
                id = UUID.randomUUID().toString(),
                customerId = bill.customerId,
                entryType = "payment_received",
                amount = bill.total,
                billId = bill.id,
                entryDate = System.currentTimeMillis(),
                note = "Reversal: Voided Bill #${bill.billNumber}",
                createdAt = System.currentTimeMillis(),
                synced = 0
            )
            db.khataDao().insertKhataEntry(reversalEntry)
            db.customerDao().adjustCustomerBalance(bill.customerId, -bill.total)
        }
    }

    suspend fun getBillDetails(billId: String): Pair<Bill?, List<BillItem>> = withContext(Dispatchers.IO) {
        val bill = db.billDao().getBillById(billId)
        val items = db.billDao().getBillItems(billId)
        Pair(bill, items)
    }

    fun getBillItemsFlow(billId: String): Flow<List<BillItem>> {
        return db.billDao().getBillItemsFlow(billId)
    }

    // --- Sales & Reports Computation ---
    suspend fun getTodaySales(): SalesSummary = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        val bills = db.billDao().getTodayBills(startOfDay, endOfDay)
        computeSalesSummary(bills)
    }

    suspend fun getMonthlySales(): SalesSummary = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis
        val now = System.currentTimeMillis()

        val bills = db.billDao().getBillsInRange(startOfMonth, now)
        computeSalesSummary(bills)
    }

    private fun computeSalesSummary(bills: List<Bill>): SalesSummary {
        var totalSales = 0.0
        var cashSales = 0.0
        var upiSales = 0.0
        var creditSales = 0.0
        var invoiceCount = 0

        for (bill in bills) {
            if (bill.billType == "quotation") continue
            totalSales += bill.total
            invoiceCount++
            when (bill.paymentMode.lowercase()) {
                "cash" -> cashSales += bill.total
                "upi" -> upiSales += bill.total
                "credit" -> creditSales += bill.total
                else -> cashSales += bill.total
            }
        }
        return SalesSummary(
            totalSales = totalSales,
            cashSales = cashSales,
            upiSales = upiSales,
            creditSales = creditSales,
            billCount = invoiceCount
        )
    }

    suspend fun getStockValuation(): Double = withContext(Dispatchers.IO) {
        val stock = db.stockDao().getAllStock()
        stock.sumOf { it.currentQty * it.purchasePrice }
    }

    suspend fun getProfitReport(): ProfitReport = withContext(Dispatchers.IO) {
        val activeBills = db.billDao().getAllBills().filter { it.status == "active" && it.billType != "quotation" }
        val allItems = db.billDao().getAllBillItems()
        val activeBillIds = activeBills.map { it.id }.toSet()

        var totalRevenue = 0.0
        var totalCost = 0.0

        for (item in allItems) {
            if (activeBillIds.contains(item.billId)) {
                totalRevenue += item.lineTotal
                totalCost += (item.qty * item.purchasePriceSnapshot)
            }
        }
        val grossProfit = totalRevenue - totalCost
        val marginPercent = if (totalRevenue > 0) (grossProfit / totalRevenue) * 100 else 0.0
        ProfitReport(totalRevenue = totalRevenue, totalCost = totalCost, grossProfit = grossProfit, marginPercent = marginPercent)
    }

    suspend fun getTopSellingItems(limit: Int = 10): List<TopSellingItem> = withContext(Dispatchers.IO) {
        val activeBills = db.billDao().getAllBills().filter { it.status == "active" && it.billType != "quotation" }
        val activeBillIds = activeBills.map { it.id }.toSet()
        val items = db.billDao().getAllBillItems().filter { activeBillIds.contains(it.billId) }

        val grouped = items.groupBy { it.stockItemId }
        grouped.map { (stockId, list) ->
            val name = list.firstOrNull()?.itemNameSnapshot ?: "Item"
            val totalQty = list.sumOf { it.qty }
            val totalAmount = list.sumOf { it.lineTotal }
            val unit = list.firstOrNull()?.unit ?: ""
            TopSellingItem(stockItemId = stockId, itemName = name, totalQty = totalQty, totalAmount = totalAmount, unit = unit)
        }.sortedByDescending { it.totalAmount }.take(limit)
    }

    // --- Data Sync / Backup Bundle ---
    suspend fun getUnsyncedCount(): Int = withContext(Dispatchers.IO) {
        val stockCount = db.stockDao().getUnsyncedStock().size
        val custCount = db.customerDao().getUnsyncedCustomers().size
        val billCount = db.billDao().getUnsyncedBills().size
        val purchaseCount = db.purchaseDao().getUnsyncedPurchases().size
        val khataCount = db.khataDao().getUnsyncedKhata().size
        val adjCount = db.stockAdjustmentDao().getUnsyncedAdjustments().size
        stockCount + custCount + billCount + purchaseCount + khataCount + adjCount
    }

    suspend fun getAllDataForBackup(): BackupDataBundle = withContext(Dispatchers.IO) {
        BackupDataBundle(
            stock = db.stockDao().getAllStock(),
            customers = db.customerDao().getAllCustomers(),
            bills = db.billDao().getAllBills(),
            billItems = db.billDao().getAllBillItems(),
            purchases = db.purchaseDao().getAllPurchases(),
            khataEntries = db.khataDao().getAllKhataEntries(),
            stockAdjustments = db.stockAdjustmentDao().getAllAdjustments()
        )
    }

    suspend fun restoreFromBackupBundle(bundle: BackupDataBundle): RestoreResult = withContext(Dispatchers.IO) {
        var restoredCount = 0
        var skippedCount = 0

        // Parents before children: Stock -> Customers -> Purchases -> Bills -> BillItems -> KhataEntries -> StockAdjustments
        val existingStockIds = db.stockDao().getAllStock().map { it.id }.toSet()
        val newStock = bundle.stock.filter { !existingStockIds.contains(it.id) }
        db.stockDao().insertStockBatch(newStock.map { it.copy(synced = 1) })
        restoredCount += newStock.size
        skippedCount += (bundle.stock.size - newStock.size)

        val existingCustIds = db.customerDao().getAllCustomers().map { it.id }.toSet()
        val newCust = bundle.customers.filter { !existingCustIds.contains(it.id) }
        db.customerDao().insertCustomerBatch(newCust.map { it.copy(synced = 1) })
        restoredCount += newCust.size
        skippedCount += (bundle.customers.size - newCust.size)

        val existingPurchases = db.purchaseDao().getAllPurchases().map { it.id }.toSet()
        val newPurchases = bundle.purchases.filter { !existingPurchases.contains(it.id) }
        db.purchaseDao().insertPurchasesBatch(newPurchases.map { it.copy(synced = 1) })
        restoredCount += newPurchases.size
        skippedCount += (bundle.purchases.size - newPurchases.size)

        val existingBills = db.billDao().getAllBills().map { it.id }.toSet()
        val newBills = bundle.bills.filter { !existingBills.contains(it.id) }
        db.billDao().insertBillsBatch(newBills.map { it.copy(synced = 1) })
        restoredCount += newBills.size
        skippedCount += (bundle.bills.size - newBills.size)

        val existingBillItemIds = db.billDao().getAllBillItems().map { it.id }.toSet()
        val newBillItems = bundle.billItems.filter { !existingBillItemIds.contains(it.id) }
        db.billDao().insertBillItems(newBillItems)
        restoredCount += newBillItems.size
        skippedCount += (bundle.billItems.size - newBillItems.size)

        val existingKhata = db.khataDao().getAllKhataEntries().map { it.id }.toSet()
        val newKhata = bundle.khataEntries.filter { !existingKhata.contains(it.id) }
        db.khataDao().insertKhataBatch(newKhata.map { it.copy(synced = 1) })
        restoredCount += newKhata.size
        skippedCount += (bundle.khataEntries.size - newKhata.size)

        val existingAdj = db.stockAdjustmentDao().getAllAdjustments().map { it.id }.toSet()
        val newAdj = bundle.stockAdjustments.filter { !existingAdj.contains(it.id) }
        db.stockAdjustmentDao().insertAdjustmentBatch(newAdj.map { it.copy(synced = 1) })
        restoredCount += newAdj.size
        skippedCount += (bundle.stockAdjustments.size - newAdj.size)

        RestoreResult(restoredCount = restoredCount, skippedCount = skippedCount)
    }

    suspend fun markAllSynced() = withContext(Dispatchers.IO) {
        val stock = db.stockDao().getUnsyncedStock().map { it.id }
        if (stock.isNotEmpty()) db.stockDao().markStockSynced(stock)

        val cust = db.customerDao().getUnsyncedCustomers().map { it.id }
        if (cust.isNotEmpty()) db.customerDao().markCustomersSynced(cust)

        val bills = db.billDao().getUnsyncedBills().map { it.id }
        if (bills.isNotEmpty()) db.billDao().markBillsSynced(bills)

        val purchases = db.purchaseDao().getUnsyncedPurchases().map { it.id }
        if (purchases.isNotEmpty()) db.purchaseDao().markPurchasesSynced(purchases)

        val khata = db.khataDao().getUnsyncedKhata().map { it.id }
        if (khata.isNotEmpty()) db.khataDao().markKhataSynced(khata)

        val adj = db.stockAdjustmentDao().getUnsyncedAdjustments().map { it.id }
        if (adj.isNotEmpty()) db.stockAdjustmentDao().markAdjustmentsSynced(adj)
    }
}

data class SalesSummary(
    val totalSales: Double = 0.0,
    val cashSales: Double = 0.0,
    val upiSales: Double = 0.0,
    val creditSales: Double = 0.0,
    val billCount: Int = 0
)

data class ProfitReport(
    val totalRevenue: Double = 0.0,
    val totalCost: Double = 0.0,
    val grossProfit: Double = 0.0,
    val marginPercent: Double = 0.0
)

data class TopSellingItem(
    val stockItemId: String,
    val itemName: String,
    val totalQty: Double,
    val totalAmount: Double,
    val unit: String
)

data class BackupDataBundle(
    val stock: List<StockItem> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val bills: List<Bill> = emptyList(),
    val billItems: List<BillItem> = emptyList(),
    val purchases: List<Purchase> = emptyList(),
    val khataEntries: List<KhataEntry> = emptyList(),
    val stockAdjustments: List<StockAdjustment> = emptyList()
)

data class RestoreResult(
    val restoredCount: Int,
    val skippedCount: Int
)

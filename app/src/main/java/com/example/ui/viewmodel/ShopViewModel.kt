package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.*
import com.example.sync.GoogleSheetSyncManager
import com.example.sync.SheetSyncStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

enum class AppScreen {
    HOME,
    STOCK,
    BILLING,
    BILL_DETAIL,
    KHATA,
    KHATA_CUSTOMER_DETAIL,
    REPORTS,
    SETTINGS,
    ONBOARDING
}

data class DraftBillItem(
    val stockItemId: String,
    val itemName: String,
    val unit: String,
    val availableQty: Double,
    val qty: Double,
    val pricePerUnit: Double,
    val purchasePrice: Double
) {
    val lineTotal: Double get() = qty * pricePerUnit
}

class ShopViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = ShopRepository(database)
    val syncManager = GoogleSheetSyncManager(application, repository, viewModelScope)

    // Navigation State
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Shop Profile & Onboarding
    val shopProfile: StateFlow<ShopProfile?> = repository.shopProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Language ("en" or "hi")
    private val _language = MutableStateFlow("en")
    val language: StateFlow<String> = _language.asStateFlow()

    // Premium License (₹49 Play Store)
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    // Stock
    val allStock: StateFlow<List<StockItem>> = repository.allStockFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val lowStockCount: StateFlow<Int> = repository.lowStockCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val lowStockItems: StateFlow<List<StockItem>> = repository.lowStockItemsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Customers & Khata
    val allCustomers: StateFlow<List<Customer>> = repository.allCustomersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val totalDues: StateFlow<Double?> = repository.totalDuesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _selectedCustomerId = MutableStateFlow<String?>(null)
    val selectedCustomerId: StateFlow<String?> = _selectedCustomerId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedCustomerEntries: StateFlow<List<KhataEntry>> = _selectedCustomerId
        .flatMapLatest { id ->
            if (id != null) repository.getKhataEntriesFlow(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bills
    val allBills: StateFlow<List<Bill>> = repository.allBillsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedBillId = MutableStateFlow<String?>(null)
    val selectedBillId: StateFlow<String?> = _selectedBillId.asStateFlow()

    private val _selectedBill = MutableStateFlow<Bill?>(null)
    val selectedBill: StateFlow<Bill?> = _selectedBill.asStateFlow()

    private val _selectedBillItems = MutableStateFlow<List<BillItem>>(emptyList())
    val selectedBillItems: StateFlow<List<BillItem>> = _selectedBillItems.asStateFlow()

    // Today's Sales Dashboard
    private val _todaySales = MutableStateFlow(SalesSummary())
    val todaySales: StateFlow<SalesSummary> = _todaySales.asStateFlow()

    // Reports States
    private val _monthlySales = MutableStateFlow(SalesSummary())
    val monthlySales: StateFlow<SalesSummary> = _monthlySales.asStateFlow()

    private val _profitReport = MutableStateFlow(ProfitReport())
    val profitReport: StateFlow<ProfitReport> = _profitReport.asStateFlow()

    private val _stockValuation = MutableStateFlow(0.0)
    val stockValuation: StateFlow<Double> = _stockValuation.asStateFlow()

    private val _topSellingItems = MutableStateFlow<List<TopSellingItem>>(emptyList())
    val topSellingItems: StateFlow<List<TopSellingItem>> = _topSellingItems.asStateFlow()

    // Date-wise Sales Ledger (What I Sold)
    private val _selectedSalesDate = MutableStateFlow(System.currentTimeMillis())
    val selectedSalesDate: StateFlow<Long> = _selectedSalesDate.asStateFlow()

    private val _dateSalesSummary = MutableStateFlow<DateSalesSummary?>(null)
    val dateSalesSummary: StateFlow<DateSalesSummary?> = _dateSalesSummary.asStateFlow()

    private val _isDateSalesLoading = MutableStateFlow(false)
    val isDateSalesLoading: StateFlow<Boolean> = _isDateSalesLoading.asStateFlow()

    private val _reportsTab = MutableStateFlow(0) // 0: What I Sold (Daily Sales), 1: Business Analytics
    val reportsTab: StateFlow<Int> = _reportsTab.asStateFlow()

    // Sync status
    val syncStatus: StateFlow<SheetSyncStatus> = syncManager.syncStatus

    // Draft Bill State
    private val _draftBillType = MutableStateFlow("invoice") // "invoice", "quotation", "receipt"
    val draftBillType: StateFlow<String> = _draftBillType.asStateFlow()

    private val _draftCustomer = MutableStateFlow<Customer?>(null)
    val draftCustomer: StateFlow<Customer?> = _draftCustomer.asStateFlow()

    private val _draftCustomerName = MutableStateFlow("Walk-in Customer")
    val draftCustomerName: StateFlow<String> = _draftCustomerName.asStateFlow()

    private val _draftCustomerPhone = MutableStateFlow("")
    val draftCustomerPhone: StateFlow<String> = _draftCustomerPhone.asStateFlow()

    private val _draftItems = MutableStateFlow<List<DraftBillItem>>(emptyList())
    val draftItems: StateFlow<List<DraftBillItem>> = _draftItems.asStateFlow()

    private val _draftDiscount = MutableStateFlow(0.0)
    val draftDiscount: StateFlow<Double> = _draftDiscount.asStateFlow()

    private val _draftGstRate = MutableStateFlow(0.0)
    val draftGstRate: StateFlow<Double> = _draftGstRate.asStateFlow()

    private val _draftPaymentMode = MutableStateFlow("cash") // "cash", "credit", "upi"
    val draftPaymentMode: StateFlow<String> = _draftPaymentMode.asStateFlow()

    // Toast/Alert message
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        checkProfileAndInitialize()
        refreshDashboardMetrics()
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    private fun checkProfileAndInitialize() {
        viewModelScope.launch {
            val profile = repository.getProfile()
            if (profile == null || !profile.isInitialized) {
                _currentScreen.value = AppScreen.ONBOARDING
            } else {
                _language.value = profile.language
                _isPremium.value = profile.isPremium
            }
        }
    }

    fun refreshDashboardMetrics() {
        viewModelScope.launch {
            _todaySales.value = repository.getTodaySales()
            _stockValuation.value = repository.getStockValuation()
            syncManager.updatePendingCount()
            loadSalesForDate(_selectedSalesDate.value)
        }
    }

    fun loadReports() {
        viewModelScope.launch {
            _todaySales.value = repository.getTodaySales()
            _monthlySales.value = repository.getMonthlySales()
            _profitReport.value = repository.getProfitReport()
            _stockValuation.value = repository.getStockValuation()
            _topSellingItems.value = repository.getTopSellingItems(10)
            loadSalesForDate(_selectedSalesDate.value)
        }
    }

    fun setReportsTab(tab: Int) {
        _reportsTab.value = tab
    }

    fun openSalesByDate(dateMillis: Long = System.currentTimeMillis()) {
        _reportsTab.value = 0
        selectSalesDate(dateMillis)
        _currentScreen.value = AppScreen.REPORTS
    }

    fun selectSalesDate(timestamp: Long) {
        _selectedSalesDate.value = timestamp
        loadSalesForDate(timestamp)
    }

    fun changeSalesDateByDays(days: Int) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _selectedSalesDate.value
            add(Calendar.DAY_OF_YEAR, days)
        }
        selectSalesDate(cal.timeInMillis)
    }

    fun selectTodaySales() {
        selectSalesDate(System.currentTimeMillis())
    }

    fun selectYesterdaySales() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.DAY_OF_YEAR, -1)
        }
        selectSalesDate(cal.timeInMillis)
    }

    fun loadSalesForDate(timestamp: Long = _selectedSalesDate.value) {
        viewModelScope.launch {
            _isDateSalesLoading.value = true
            try {
                val summary = repository.getSalesForDate(timestamp)
                _dateSalesSummary.value = summary
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isDateSalesLoading.value = false
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        if (screen == AppScreen.REPORTS) {
            loadReports()
        }
        _currentScreen.value = screen
    }

    fun setLanguage(lang: String) {
        _language.value = lang
        viewModelScope.launch {
            val current = repository.getProfile() ?: ShopProfile()
            repository.saveProfile(current.copy(language = lang))
        }
    }

    fun purchasePremiumLicense() {
        viewModelScope.launch {
            _isPremium.value = true
            val current = repository.getProfile() ?: ShopProfile()
            repository.saveProfile(current.copy(isPremium = true))
            showMessage("Lifetime License activated! Thank you for supporting Dukan Khata.")
        }
    }

    fun restorePurchase() {
        viewModelScope.launch {
            _isPremium.value = true
            val current = repository.getProfile() ?: ShopProfile()
            repository.saveProfile(current.copy(isPremium = true))
            showMessage("Purchase restored successfully!")
        }
    }

    // --- Onboarding ---
    fun completeOnboarding(
        shopName: String,
        ownerName: String,
        phone: String,
        gstin: String,
        address: String
    ) {
        viewModelScope.launch {
            val profile = ShopProfile(
                id = 1,
                shopName = shopName.trim(),
                ownerName = ownerName.trim(),
                phone = phone.trim(),
                gstin = gstin.trim().uppercase(),
                address = address.trim(),
                isInitialized = true,
                language = _language.value,
                isPremium = _isPremium.value
            )
            repository.saveProfile(profile)
            _currentScreen.value = AppScreen.HOME
            showMessage("Shop Profile Created!")
            refreshDashboardMetrics()
        }
    }

    // --- Stock Management ---
    fun addStockItem(
        name: String,
        unit: String,
        openingQty: Double,
        purchasePrice: Double,
        sellingPrice: Double,
        lowStockThreshold: Double
    ) {
        viewModelScope.launch {
            repository.addStockItem(name, unit, openingQty, purchasePrice, sellingPrice, lowStockThreshold)
            refreshDashboardMetrics()
            syncManager.triggerRealtimeSync()
            showMessage("Item added to stock")
        }
    }

    fun updateStockItem(item: StockItem) {
        viewModelScope.launch {
            repository.updateStockItem(item)
            refreshDashboardMetrics()
            syncManager.triggerRealtimeSync()
            showMessage("Stock item updated")
        }
    }

    fun restockItem(stockItemId: String, qty: Double, purchasePrice: Double, supplier: String) {
        viewModelScope.launch {
            repository.restockItem(stockItemId, qty, purchasePrice, supplier)
            refreshDashboardMetrics()
            syncManager.triggerRealtimeSync()
            showMessage("Stock replenished (+${qty})")
        }
    }

    fun adjustStock(stockItemId: String, qtyChange: Double, reason: String) {
        viewModelScope.launch {
            repository.adjustStock(stockItemId, qtyChange, reason)
            refreshDashboardMetrics()
            syncManager.triggerRealtimeSync()
            showMessage("Stock adjusted (${qtyChange})")
        }
    }

    fun archiveStockItem(stockItemId: String) {
        viewModelScope.launch {
            repository.archiveStockItem(stockItemId)
            refreshDashboardMetrics()
            syncManager.triggerRealtimeSync()
            showMessage("Item archived")
        }
    }

    // --- Khata Management ---
    fun addCustomer(name: String, phone: String, initialDue: Double = 0.0) {
        viewModelScope.launch {
            repository.addCustomer(name, phone, initialDue)
            syncManager.triggerRealtimeSync()
            showMessage("Customer added to Khata")
        }
    }

    fun selectCustomer(customerId: String) {
        _selectedCustomerId.value = customerId
        _currentScreen.value = AppScreen.KHATA_CUSTOMER_DETAIL
    }

    fun recordKhataPayment(customerId: String, amount: Double, note: String) {
        viewModelScope.launch {
            repository.recordKhataPayment(customerId, amount, note)
            refreshDashboardMetrics()
            syncManager.triggerRealtimeSync()
            showMessage("Payment of ₹$amount recorded")
        }
    }

    fun recordKhataCredit(customerId: String, amount: Double, note: String) {
        viewModelScope.launch {
            repository.recordKhataCredit(customerId, amount, note)
            refreshDashboardMetrics()
            syncManager.triggerRealtimeSync()
            showMessage("Credit of ₹$amount recorded")
        }
    }

    // --- Billing Draft ---
    fun startNewBill(preselectedCustomer: Customer? = null) {
        _draftBillType.value = "invoice"
        _draftCustomer.value = preselectedCustomer
        _draftCustomerName.value = preselectedCustomer?.name ?: "Walk-in Customer"
        _draftCustomerPhone.value = preselectedCustomer?.phone ?: ""
        _draftItems.value = emptyList()
        _draftDiscount.value = 0.0
        val profile = shopProfile.value
        _draftGstRate.value = if (!profile?.gstin.isNullOrBlank()) 18.0 else 0.0
        _draftPaymentMode.value = if (preselectedCustomer != null) "credit" else "cash"
        _currentScreen.value = AppScreen.BILLING
    }

    fun setDraftBillType(type: String) {
        _draftBillType.value = type
    }

    fun setDraftCustomer(customer: Customer?) {
        _draftCustomer.value = customer
        if (customer != null) {
            _draftCustomerName.value = customer.name
            _draftCustomerPhone.value = customer.phone
        } else {
            _draftCustomerName.value = "Walk-in Customer"
            _draftCustomerPhone.value = ""
        }
    }

    fun setDraftCustomerManual(name: String, phone: String) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            val trimmedPhone = phone.trim()
            if (trimmedName.isBlank() || trimmedName.equals("Walk-in Customer", ignoreCase = true)) {
                setDraftCustomer(null)
                return@launch
            }
            val existing = repository.allCustomersFlow.first().find {
                it.name.equals(trimmedName, ignoreCase = true) ||
                    (trimmedPhone.isNotBlank() && it.phone == trimmedPhone)
            }
            val target = existing ?: repository.addCustomer(trimmedName, trimmedPhone, 0.0)
            setDraftCustomer(target)
        }
    }

    fun setOrCreateCreditCustomer(name: String, phone: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            val trimmedPhone = phone.trim()
            if (trimmedName.isBlank() || trimmedName.equals("Walk-in Customer", ignoreCase = true)) {
                showMessage("Please enter customer name for credit sale")
                return@launch
            }

            val existing = repository.allCustomersFlow.first().find {
                it.name.equals(trimmedName, ignoreCase = true) ||
                    (trimmedPhone.isNotBlank() && it.phone == trimmedPhone)
            }
            val target = existing ?: repository.addCustomer(trimmedName, trimmedPhone, 0.0)
            setDraftCustomer(target)
            _draftPaymentMode.value = "credit"
            syncManager.triggerRealtimeSync()
            showMessage("Customer linked for Credit: ${target.name}")
            onDone()
        }
    }

    fun setDraftPaymentMode(mode: String) {
        _draftPaymentMode.value = mode
    }

    fun setDraftDiscount(discount: Double) {
        _draftDiscount.value = discount
    }

    fun setDraftGstRate(rate: Double) {
        _draftGstRate.value = rate
    }

    fun addDraftItem(stockItem: StockItem, qty: Double, customPrice: Double? = null) {
        val current = _draftItems.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.stockItemId == stockItem.id }
        val price = customPrice ?: stockItem.sellingPrice

        if (existingIndex >= 0) {
            val existing = current[existingIndex]
            current[existingIndex] = existing.copy(
                qty = existing.qty + qty,
                pricePerUnit = price
            )
        } else {
            current.add(
                DraftBillItem(
                    stockItemId = stockItem.id,
                    itemName = stockItem.name,
                    unit = stockItem.unit,
                    availableQty = stockItem.currentQty,
                    qty = qty,
                    pricePerUnit = price,
                    purchasePrice = stockItem.purchasePrice
                )
            )
        }
        _draftItems.value = current
    }

    fun addDraftItemCustom(
        name: String,
        unit: String,
        qty: Double,
        price: Double,
        purchasePrice: Double = 0.0
    ) {
        val current = _draftItems.value.toMutableList()
        val generatedId = UUID.randomUUID().toString()
        current.add(
            DraftBillItem(
                stockItemId = generatedId,
                itemName = name.trim(),
                unit = unit,
                availableQty = 0.0,
                qty = qty,
                pricePerUnit = price,
                purchasePrice = purchasePrice
            )
        )
        _draftItems.value = current
    }

    fun updateDraftItem(stockItemId: String, newQty: Double, newPrice: Double) {
        if (newQty <= 0) {
            removeDraftItem(stockItemId)
            return
        }
        val current = _draftItems.value.map {
            if (it.stockItemId == stockItemId) it.copy(qty = newQty, pricePerUnit = newPrice) else it
        }
        _draftItems.value = current
    }

    fun updateDraftItemQty(stockItemId: String, newQty: Double) {
        if (newQty <= 0) {
            removeDraftItem(stockItemId)
            return
        }
        val current = _draftItems.value.map {
            if (it.stockItemId == stockItemId) it.copy(qty = newQty) else it
        }
        _draftItems.value = current
    }

    fun updateDraftItemPrice(stockItemId: String, newPrice: Double) {
        val current = _draftItems.value.map {
            if (it.stockItemId == stockItemId) it.copy(pricePerUnit = newPrice) else it
        }
        _draftItems.value = current
    }

    fun removeDraftItem(stockItemId: String) {
        _draftItems.value = _draftItems.value.filter { it.stockItemId != stockItemId }
    }

    fun checkNegativeStockItems(): List<DraftBillItem> {
        return _draftItems.value.filter { it.qty > it.availableQty }
    }

    fun saveDraftBill(onSaved: (Bill, List<BillItem>) -> Unit) {
        val items = _draftItems.value
        if (items.isEmpty()) {
            showMessage("Please add at least one item")
            return
        }

        viewModelScope.launch {
            val subtotal = items.sumOf { it.lineTotal }
            val discount = _draftDiscount.value.coerceAtMost(subtotal)
            val taxableAmount = subtotal - discount
            val gstRate = _draftGstRate.value
            val gstAmount = if (gstRate > 0) (taxableAmount * gstRate) / 100.0 else 0.0
            val total = taxableAmount + gstAmount

            // Generate clean bill number like INV-1001
            val billCount = repository.allBillsFlow.first().size + 1
            val prefix = when (_draftBillType.value) {
                "quotation" -> "EST"
                "receipt" -> "REC"
                else -> "INV"
            }
            val billNumber = "$prefix-${1000 + billCount}"

            val billId = UUID.randomUUID().toString()
            val bill = Bill(
                id = billId,
                billNumber = billNumber,
                billType = _draftBillType.value,
                customerId = _draftCustomer.value?.id,
                customerNameSnapshot = _draftCustomerName.value,
                customerPhoneSnapshot = _draftCustomerPhone.value,
                billDate = System.currentTimeMillis(),
                subtotal = subtotal,
                gstAmount = gstAmount,
                gstRatePercent = gstRate,
                discount = discount,
                total = total,
                paymentMode = _draftPaymentMode.value,
                status = "active",
                createdAt = System.currentTimeMillis(),
                synced = 0
            )

            val billItems = items.map { draft ->
                BillItem(
                    id = UUID.randomUUID().toString(),
                    billId = billId,
                    stockItemId = draft.stockItemId,
                    itemNameSnapshot = draft.itemName,
                    qty = draft.qty,
                    unit = draft.unit,
                    pricePerUnit = draft.pricePerUnit,
                    purchasePriceSnapshot = draft.purchasePrice,
                    lineTotal = draft.lineTotal
                )
            }

            repository.createBill(bill, billItems)
            refreshDashboardMetrics()
            syncManager.triggerRealtimeSync()
            _selectedBillId.value = billId
            _selectedBill.value = bill
            _selectedBillItems.value = billItems
            _currentScreen.value = AppScreen.BILL_DETAIL
            onSaved(bill, billItems)
            showMessage("Bill #$billNumber created successfully!")
        }
    }

    fun openBillDetail(bill: Bill) {
        viewModelScope.launch {
            _selectedBillId.value = bill.id
            _selectedBill.value = bill
            val (_, items) = repository.getBillDetails(bill.id)
            _selectedBillItems.value = items
            _currentScreen.value = AppScreen.BILL_DETAIL
        }
    }

    fun voidCurrentBill() {
        val bill = _selectedBill.value ?: return
        viewModelScope.launch {
            repository.voidBill(bill.id)
            val (updatedBill, items) = repository.getBillDetails(bill.id)
            _selectedBill.value = updatedBill
            _selectedBillItems.value = items
            refreshDashboardMetrics()
            syncManager.triggerRealtimeSync()
            showMessage("Bill #${bill.billNumber} voided and stock restored")
        }
    }
}

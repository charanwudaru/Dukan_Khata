package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.theme.dukanTextFieldColors
import com.example.util.QuantityUtils
import com.example.util.UnitCategory
import com.example.data.model.Customer
import com.example.data.model.StockItem
import com.example.pdf.InvoicePdfGenerator
import com.example.ui.strings.LocaleStrings
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.DraftBillItem
import com.example.ui.viewmodel.ShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang by viewModel.language.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()
    val allStock by viewModel.allStock.collectAsState()
    val allCustomers by viewModel.allCustomers.collectAsState()

    val billType by viewModel.draftBillType.collectAsState()
    val draftCustomer by viewModel.draftCustomer.collectAsState()
    val customerName by viewModel.draftCustomerName.collectAsState()
    val customerPhone by viewModel.draftCustomerPhone.collectAsState()
    val draftItems by viewModel.draftItems.collectAsState()
    val discount by viewModel.draftDiscount.collectAsState()
    val gstRate by viewModel.draftGstRate.collectAsState()
    val paymentMode by viewModel.draftPaymentMode.collectAsState()

    var showAddItemDialog by remember { mutableStateOf(false) }
    var showCustomerDialog by remember { mutableStateOf(false) }
    var showCreditCustomerDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<DraftBillItem?>(null) }
    var showNegativeStockDialog by remember { mutableStateOf(false) }
    var negativeStockWarningMsg by remember { mutableStateOf("") }
    var pendingSaveAfterWarning by remember { mutableStateOf(false) }

    val subtotal = draftItems.sumOf { it.lineTotal }
    val taxable = (subtotal - discount).coerceAtLeast(0.0)
    val gstAmount = if (gstRate > 0) (taxable * gstRate) / 100.0 else 0.0
    val grandTotal = taxable + gstAmount

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = LocaleStrings.get("create_bill", lang),
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.HOME) },
                        modifier = Modifier.testTag("billing_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = BrandNavy)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${draftItems.size} ${LocaleStrings.get("items", lang)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Text(
                                text = formatRupee(grandTotal),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy
                            )
                        }

                        Button(
                            onClick = {
                                if (draftItems.isEmpty()) {
                                    viewModel.showMessage("Please add at least one item")
                                    return@Button
                                }
                                val isWalkIn = draftCustomer == null || customerName.isBlank() ||
                                    customerName.equals("Walk-in Customer", ignoreCase = true) ||
                                    customerName == LocaleStrings.get("walk_in", lang)
                                if (paymentMode == "credit" && isWalkIn) {
                                    showCreditCustomerDialog = true
                                    viewModel.showMessage("Please enter customer name to record credit in Khata")
                                    return@Button
                                }
                                viewModel.saveDraftBill { savedBill, savedItems ->
                                    InvoicePdfGenerator.generateBillPdf(context, shopProfile, savedBill, savedItems)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("save_bill_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(LocaleStrings.get("save_bill", lang), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandSurface)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp)
        ) {
            // Bill Type Segmented Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val types = listOf(
                        "invoice" to LocaleStrings.get("invoice", lang),
                        "quotation" to LocaleStrings.get("quotation", lang),
                        "receipt" to LocaleStrings.get("receipt", lang)
                    )
                    types.forEach { (type, label) ->
                        val selected = billType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) BrandNavy else Color.Transparent)
                                .clickable { viewModel.setDraftBillType(type) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else Color.DarkGray,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Customer Selector Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCustomerDialog = true }
                        .testTag("select_customer_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE8EAF6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = BrandNavy)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = LocaleStrings.get("customer", lang),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Gray
                            )
                            Text(
                                text = customerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            if (customerPhone.isNotBlank()) {
                                Text(
                                    text = customerPhone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.DarkGray
                                )
                            }
                        }
                        TextButton(onClick = { showCustomerDialog = true }) {
                            Text(LocaleStrings.get("change", lang), color = BrandBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Items Header & Add Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${LocaleStrings.get("items", lang)} (${draftItems.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Button(
                        onClick = { showAddItemDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8EAF6)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_item_to_bill_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = BrandNavy, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(LocaleStrings.get("add_items_to_bill", lang), color = BrandNavy, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Draft Items List
            if (draftItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddItemDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(42.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = LocaleStrings.get("tap_add_items", lang),
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                items(draftItems) { item ->
                    DraftItemCard(
                        item = item,
                        onEdit = { editingItem = item },
                        onQtyChange = { newQty -> viewModel.updateDraftItemQty(item.stockItemId, newQty) },
                        onDelete = { viewModel.removeDraftItem(item.stockItemId) }
                    )
                }
            }

            // Calculation & Discount / GST
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = LocaleStrings.get("bill_summary", lang),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Subtotal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(LocaleStrings.get("subtotal", lang), color = Color(0xFF475569))
                            Text(formatRupee(subtotal), fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Discount Row with both ₹ and % modes
                        var isDiscountPercent by remember { mutableStateOf(false) }
                        var discountInput by remember { mutableStateOf(if (discount > 0) discount.toString() else "") }

                        LaunchedEffect(subtotal, isDiscountPercent, discountInput) {
                            if (isDiscountPercent) {
                                val pct = discountInput.toDoubleOrNull() ?: 0.0
                                val calcDiscount = (subtotal * pct) / 100.0
                                viewModel.setDraftDiscount(calcDiscount)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(LocaleStrings.get("discount", lang), color = Color(0xFF475569))
                                if (isDiscountPercent && discount > 0) {
                                    Text(
                                        text = "(-${formatRupee(discount)})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Toggle ₹ and %
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF1F5F9))
                                        .padding(2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (!isDiscountPercent) BrandNavy else Color.Transparent)
                                            .clickable {
                                                isDiscountPercent = false
                                                val amt = discountInput.toDoubleOrNull() ?: 0.0
                                                viewModel.setDraftDiscount(amt)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            "₹",
                                            fontWeight = FontWeight.Bold,
                                            color = if (!isDiscountPercent) Color.White else Color.DarkGray,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isDiscountPercent) BrandNavy else Color.Transparent)
                                            .clickable {
                                                isDiscountPercent = true
                                                val pct = discountInput.toDoubleOrNull() ?: 0.0
                                                val calcDiscount = (subtotal * pct) / 100.0
                                                viewModel.setDraftDiscount(calcDiscount)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            "%",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDiscountPercent) Color.White else Color.DarkGray,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = discountInput,
                                    onValueChange = {
                                        discountInput = it
                                        val entered = it.toDoubleOrNull() ?: 0.0
                                        if (isDiscountPercent) {
                                            val calc = (subtotal * entered) / 100.0
                                            viewModel.setDraftDiscount(calc)
                                        } else {
                                            viewModel.setDraftDiscount(entered)
                                        }
                                    },
                                    modifier = Modifier
                                        .width(110.dp)
                                        .testTag("discount_input"),
                                    placeholder = { Text(if (isDiscountPercent) "0%" else "0") },
                                    singleLine = true,
                                    colors = dukanTextFieldColors(),
                                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // GST Row (if GSTIN present or rate > 0)
                        if (!shopProfile?.gstin.isNullOrBlank() || gstRate > 0) {
                            var gstText by remember { mutableStateOf(if (gstRate > 0) gstRate.toString() else "0") }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${LocaleStrings.get("gst_rate", lang)} (${formatRupee(gstAmount)})", color = Color(0xFF475569))
                                OutlinedTextField(
                                    value = gstText,
                                    onValueChange = {
                                        gstText = it
                                        val r = it.toDoubleOrNull() ?: 0.0
                                        viewModel.setDraftGstRate(r)
                                    },
                                    modifier = Modifier.width(110.dp),
                                    placeholder = { Text("0%") },
                                    singleLine = true,
                                    colors = dukanTextFieldColors(),
                                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                        // Net Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = LocaleStrings.get("total_payable", lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy
                            )
                            Text(
                                text = formatRupee(grandTotal),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy
                            )
                        }
                    }
                }
            }

            // Payment Mode Selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = LocaleStrings.get("payment_mode", lang),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val modes = listOf(
                                Triple("cash", LocaleStrings.get("cash", lang), Icons.Default.Money),
                                Triple("upi", LocaleStrings.get("upi", lang), Icons.Default.QrCode),
                                Triple("credit", LocaleStrings.get("credit", lang), Icons.Default.AccountBalanceWallet)
                            )
                            modes.forEach { (mode, label, icon) ->
                                val isSelected = paymentMode == mode
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (mode == "credit") {
                                                val isWalkIn = draftCustomer == null || customerName.isBlank() ||
                                                    customerName.equals("Walk-in Customer", ignoreCase = true) ||
                                                    customerName == LocaleStrings.get("walk_in", lang)
                                                if (isWalkIn) {
                                                    showCreditCustomerDialog = true
                                                } else {
                                                    viewModel.setDraftPaymentMode("credit")
                                                }
                                            } else {
                                                viewModel.setDraftPaymentMode(mode)
                                            }
                                        }
                                        .testTag("payment_mode_$mode"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) BrandNavy else Color(0xFFF1F5F9)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = if (isSelected) Color.White else Color.DarkGray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color.DarkGray,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Item Dialog
    if (showAddItemDialog) {
        AddItemToBillDialog(
            stockList = allStock,
            onDismiss = { showAddItemDialog = false },
            onItemAdded = { stockItem, qty, price ->
                viewModel.addDraftItem(stockItem, qty, price)
                showAddItemDialog = false
            },
            onCustomItemAdded = { name, unit, qty, price ->
                viewModel.addDraftItemCustom(name, unit, qty, price)
                showAddItemDialog = false
            },
            onAddNewStockClick = {
                showAddItemDialog = false
                viewModel.navigateTo(AppScreen.STOCK)
            }
        )
    }

    // Customer Selection Dialog
    if (showCustomerDialog) {
        CustomerSelectDialog(
            customerList = allCustomers,
            currentSelected = draftCustomer,
            onDismiss = { showCustomerDialog = false },
            onSelectCustomer = { cust ->
                viewModel.setDraftCustomer(cust)
                showCustomerDialog = false
            },
            onSelectWalkIn = {
                viewModel.setDraftCustomer(null)
                showCustomerDialog = false
            },
            onAddCustomer = { name, phone ->
                viewModel.addCustomer(name, phone)
                viewModel.setDraftCustomerManual(name, phone)
                showCustomerDialog = false
            }
        )
    }

    // Credit Customer Dialog (prompts name when paying with Credit / Khata)
    if (showCreditCustomerDialog) {
        CreditCustomerDialog(
            customerList = allCustomers,
            onDismiss = {
                showCreditCustomerDialog = false
                val isStillWalkIn = draftCustomer == null || customerName.isBlank() ||
                    customerName.equals("Walk-in Customer", ignoreCase = true) ||
                    customerName == LocaleStrings.get("walk_in", lang)
                if (isStillWalkIn) {
                    viewModel.setDraftPaymentMode("cash")
                }
            },
            onConfirm = { name, phone ->
                viewModel.setOrCreateCreditCustomer(name, phone) {
                    showCreditCustomerDialog = false
                }
            },
            onSelectExisting = { cust ->
                viewModel.setDraftCustomer(cust)
                viewModel.setDraftPaymentMode("credit")
                showCreditCustomerDialog = false
                viewModel.showMessage("Selected ${cust.name} for Credit")
            }
        )
    }

    // Edit Cart Item Dialog
    editingItem?.let { itemToEdit ->
        EditCartItemDialog(
            item = itemToEdit,
            onDismiss = { editingItem = null },
            onUpdate = { newQty, newPrice ->
                viewModel.updateDraftItem(itemToEdit.stockItemId, newQty, newPrice)
                editingItem = null
            },
            onDelete = {
                viewModel.removeDraftItem(itemToEdit.stockItemId)
                editingItem = null
            }
        )
    }

    // Negative Stock Warning Dialog
    if (showNegativeStockDialog) {
        AlertDialog(
            onDismissRequest = { showNegativeStockDialog = false },
            title = {
                Text(LocaleStrings.get("negative_stock_warning", lang), fontWeight = FontWeight.Bold, color = DangerRed)
            },
            text = {
                Text(negativeStockWarningMsg)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNegativeStockDialog = false
                        viewModel.saveDraftBill { savedBill, savedItems ->
                            InvoicePdfGenerator.generateBillPdf(context, shopProfile, savedBill, savedItems)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text(LocaleStrings.get("continue_anyway", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNegativeStockDialog = false }) {
                    Text(LocaleStrings.get("cancel", lang))
                }
            }
        )
    }
}

@Composable
fun DraftItemCard(
    item: DraftBillItem,
    onEdit: () -> Unit,
    onQtyChange: (Double) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.itemName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Stock: ${item.availableQty} ${item.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.qty > item.availableQty) DangerRed else Color(0xFF64748B)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Item", tint = BrandNavy, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity Counter with +/- buttons and human-friendly display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                ) {
                    IconButton(
                        onClick = {
                            val category = QuantityUtils.getCategory(item.unit)
                            val step = if (category == UnitCategory.WEIGHT && item.qty <= 1.0) 0.1 else 1.0
                            val newQ = (item.qty - step).coerceAtLeast(0.0)
                            onQtyChange(newQ)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp))
                    }

                    Text(
                        text = QuantityUtils.formatSmartQuantity(item.qty, item.unit),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    IconButton(
                        onClick = {
                            val category = QuantityUtils.getCategory(item.unit)
                            val step = if (category == UnitCategory.WEIGHT && item.qty < 1.0) 0.1 else 1.0
                            onQtyChange(item.qty + step)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(18.dp))
                    }
                }

                // Price and Line Total
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Rate: ₹${item.pricePerUnit}/${item.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = formatRupee(item.lineTotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCartItemDialog(
    item: DraftBillItem,
    onDismiss: () -> Unit,
    onUpdate: (Double, Double) -> Unit,
    onDelete: () -> Unit
) {
    val category = QuantityUtils.getCategory(item.unit)
    val defaultSubUnit = when (category) {
        UnitCategory.WEIGHT -> if (item.qty < 1.0) "g" else "kg"
        UnitCategory.LIQUID -> if (item.qty < 1.0) "ml" else "l"
        UnitCategory.DOZEN -> if (item.qty % 1.0 != 0.0) "pcs" else "dozen"
        else -> item.unit
    }

    var selectedSubUnit by remember { mutableStateOf(defaultSubUnit) }
    val initialInput = when (defaultSubUnit) {
        "g" -> kotlin.math.round((item.qty * 1000.0)).toInt().toString()
        "ml" -> kotlin.math.round((item.qty * 1000.0)).toInt().toString()
        "pcs" -> kotlin.math.round((item.qty * 12.0)).toInt().toString()
        else -> if (item.qty % 1.0 == 0.0) item.qty.toInt().toString() else item.qty.toString()
    }
    var qtyInput by remember { mutableStateOf(initialInput) }
    var priceInput by remember { mutableStateOf(item.pricePerUnit.toString()) }

    val calculatedQty = QuantityUtils.parseQuantity(qtyInput, item.unit, selectedSubUnit)
    val calculatedPrice = priceInput.toDoubleOrNull() ?: item.pricePerUnit
    val calculatedTotal = calculatedQty * calculatedPrice

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Edit ${item.itemName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandNavy
                        )
                        Text(
                            text = "Stock: ${item.availableQty} ${item.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = DangerRed)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Unit Sub-Category Segmented Switcher (e.g. kg vs g)
                if (category == UnitCategory.WEIGHT) {
                    Text("Unit Type:", style = MaterialTheme.typography.labelMedium, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("kg" to "Kilograms (kg)", "g" to "Grams (g)").forEach { (code, label) ->
                            val isSel = selectedSubUnit == code
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    if (selectedSubUnit != code) {
                                        selectedSubUnit = code
                                        val curNum = qtyInput.toDoubleOrNull() ?: 0.0
                                        if (code == "g" && curNum < 50.0) {
                                            qtyInput = kotlin.math.round(curNum * 1000.0).toInt().toString()
                                        } else if (code == "kg" && curNum >= 100.0) {
                                            val converted = curNum / 1000.0
                                            qtyInput = if (converted % 1.0 == 0.0) converted.toInt().toString() else converted.toString()
                                        }
                                    }
                                },
                                label = { Text(label, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandNavy,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (category == UnitCategory.LIQUID) {
                    Text("Unit Type:", style = MaterialTheme.typography.labelMedium, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("l" to "Litres (L)", "ml" to "Millilitres (ml)").forEach { (code, label) ->
                            val isSel = selectedSubUnit == code
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    if (selectedSubUnit != code) {
                                        selectedSubUnit = code
                                        val curNum = qtyInput.toDoubleOrNull() ?: 0.0
                                        if (code == "ml" && curNum < 50.0) {
                                            qtyInput = kotlin.math.round(curNum * 1000.0).toInt().toString()
                                        } else if (code == "l" && curNum >= 100.0) {
                                            val converted = curNum / 1000.0
                                            qtyInput = if (converted % 1.0 == 0.0) converted.toInt().toString() else converted.toString()
                                        }
                                    }
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandNavy,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Quick Preset Chips
                Text("Quick Presets:", style = MaterialTheme.typography.labelMedium, color = Color(0xFF475569))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuantityUtils.getPresetChips(item.unit).forEach { chip ->
                        SuggestionChip(
                            onClick = {
                                if (category == UnitCategory.WEIGHT) {
                                    if (chip.qtyInBaseUnit < 1.0) {
                                        selectedSubUnit = "g"
                                        qtyInput = kotlin.math.round(chip.qtyInBaseUnit * 1000.0).toInt().toString()
                                    } else {
                                        selectedSubUnit = "kg"
                                        qtyInput = chip.qtyInBaseUnit.toInt().toString()
                                    }
                                } else if (category == UnitCategory.LIQUID) {
                                    if (chip.qtyInBaseUnit < 1.0) {
                                        selectedSubUnit = "ml"
                                        qtyInput = kotlin.math.round(chip.qtyInBaseUnit * 1000.0).toInt().toString()
                                    } else {
                                        selectedSubUnit = "l"
                                        qtyInput = chip.qtyInBaseUnit.toInt().toString()
                                    }
                                } else {
                                    qtyInput = if (chip.qtyInBaseUnit % 1.0 == 0.0) chip.qtyInBaseUnit.toInt().toString() else chip.qtyInBaseUnit.toString()
                                }
                            },
                            label = { Text(chip.label, fontSize = 12.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Quantity Input
                OutlinedTextField(
                    value = qtyInput,
                    onValueChange = { qtyInput = it },
                    label = { Text("Quantity (e.g. 500g, 250, 1.5)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_cart_qty_input"),
                    singleLine = true,
                    colors = dukanTextFieldColors(),
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Price Input
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { priceInput = it },
                    label = { Text("Selling Price per ${item.unit} (₹)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_cart_price_input"),
                    singleLine = true,
                    colors = dukanTextFieldColors(),
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Live Calculated Total Display Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${QuantityUtils.formatSmartQuantity(calculatedQty, item.unit)} @ ₹${calculatedPrice}/${item.unit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Item Total",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        }
                        Text(
                            text = formatRupee(calculatedTotal),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = BrandNavy
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (calculatedQty > 0) {
                                onUpdate(calculatedQty, calculatedPrice)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                        modifier = Modifier.testTag("save_edit_cart_item_btn")
                    ) {
                        Text("Update Item")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemToBillDialog(
    stockList: List<StockItem>,
    onDismiss: () -> Unit,
    onItemAdded: (StockItem, Double, Double) -> Unit,
    onCustomItemAdded: (String, String, Double, Double) -> Unit,
    onAddNewStockClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStockItem by remember { mutableStateOf<StockItem?>(null) }
    var qtyText by remember { mutableStateOf("1") }
    var priceText by remember { mutableStateOf("") }
    var selectedSubUnit by remember { mutableStateOf<String?>(null) }

    // Direct Custom Bill Item
    var isCustomMode by remember { mutableStateOf(false) }
    var customItemName by remember { mutableStateOf("") }
    var customItemUnit by remember { mutableStateOf("pcs") }
    var customItemQty by remember { mutableStateOf("1") }
    var customItemPrice by remember { mutableStateOf("") }

    val filtered = stockList.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header & Mode Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isCustomMode) "Bill New Item" else if (selectedStockItem == null) "Select Stock Item" else "Add ${selectedStockItem?.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                if (selectedStockItem == null) {
                    // Mode Switcher: Stock vs Custom
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (!isCustomMode) BrandNavy else Color.Transparent)
                                .clickable { isCustomMode = false }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "From Stock (${stockList.size})",
                                fontWeight = FontWeight.Bold,
                                color = if (!isCustomMode) Color.White else Color.DarkGray,
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isCustomMode) BrandNavy else Color.Transparent)
                                .clickable {
                                    isCustomMode = true
                                    if (customItemName.isBlank() && searchQuery.isNotBlank()) {
                                        customItemName = searchQuery
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "+ Bill New Item",
                                fontWeight = FontWeight.Bold,
                                color = if (isCustomMode) Color.White else Color.DarkGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (isCustomMode) {
                    // Form to bill a new item directly even without stock
                    OutlinedTextField(
                        value = customItemName,
                        onValueChange = { customItemName = it },
                        label = { Text("Item Name *") },
                        placeholder = { Text("e.g. Basmati Rice, Shampoo, Maggi") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = dukanTextFieldColors(),
                        textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Unit:", style = MaterialTheme.typography.labelMedium, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("pcs", "kg", "g", "packet", "l", "ml", "box").forEach { u ->
                            val isSel = customItemUnit == u
                            FilterChip(
                                selected = isSel,
                                onClick = { customItemUnit = u },
                                label = { Text(u, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandNavy,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customItemQty,
                            onValueChange = { customItemQty = it },
                            label = { Text("Qty ($customItemUnit) *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = dukanTextFieldColors(),
                            textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        OutlinedTextField(
                            value = customItemPrice,
                            onValueChange = { customItemPrice = it },
                            label = { Text("Price (₹) *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = dukanTextFieldColors(),
                            textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    val cQty = customItemQty.toDoubleOrNull() ?: 1.0
                    val cPrice = customItemPrice.toDoubleOrNull() ?: 0.0
                    val cTotal = cQty * cPrice

                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total: ${formatRupee(cTotal)}", fontWeight = FontWeight.Bold, color = BrandNavy)
                            Text("Will be added to Stock on Bill", style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isCustomMode = false }) { Text("Back to Stock") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (customItemName.isNotBlank() && cPrice > 0 && cQty > 0) {
                                    onCustomItemAdded(customItemName, customItemUnit, cQty, cPrice)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                            enabled = customItemName.isNotBlank() && cPrice > 0
                        ) {
                            Text("Add to Bill")
                        }
                    }
                } else if (selectedStockItem == null) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search items...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = dukanTextFieldColors(),
                        textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Medium)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (searchQuery.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEFF6FF))
                                .clickable {
                                    customItemName = searchQuery
                                    isCustomMode = true
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("+ Bill '$searchQuery' directly", fontWeight = FontWeight.Bold, color = BrandBlue, fontSize = 13.sp)
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (filtered.isEmpty() && searchQuery.isBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No stock items yet", color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    isCustomMode = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                            ) {
                                Text("+ Bill New Item Directly")
                            }
                        }
                    } else if (filtered.isEmpty() && searchQuery.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Item not found in stock", color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    customItemName = searchQuery
                                    isCustomMode = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                            ) {
                                Text("+ Bill '$searchQuery' as New Item")
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filtered) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF8FAFC))
                                        .clickable {
                                            selectedStockItem = item
                                            priceText = item.sellingPrice.toString()
                                            selectedSubUnit = when (QuantityUtils.getCategory(item.unit)) {
                                                UnitCategory.WEIGHT -> "kg"
                                                UnitCategory.LIQUID -> "l"
                                                UnitCategory.DOZEN -> "dozen"
                                                else -> item.unit
                                            }
                                            qtyText = "1"
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = item.name, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        Text(text = "Stock: ${item.currentQty} ${item.unit}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                    }
                                    Text(text = "₹${item.sellingPrice}", fontWeight = FontWeight.Bold, color = BrandBlue)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close")
                    }
                } else {
                    val item = selectedStockItem!!
                    val category = QuantityUtils.getCategory(item.unit)
                    val activeSubUnit = selectedSubUnit ?: item.unit

                    val calculatedQty = QuantityUtils.parseQuantity(qtyText, item.unit, activeSubUnit)
                    val calculatedPrice = priceText.toDoubleOrNull() ?: item.sellingPrice
                    val calculatedTotal = calculatedQty * calculatedPrice

                    Text(
                        text = "Available: ${item.currentQty} ${item.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Unit category switcher (e.g. kg vs g)
                    if (category == UnitCategory.WEIGHT) {
                        Text("Unit Type:", style = MaterialTheme.typography.labelMedium, color = Color(0xFF475569))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("kg" to "Kilograms (kg)", "g" to "Grams (g)").forEach { (code, label) ->
                                val isSel = activeSubUnit == code
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        if (activeSubUnit != code) {
                                            selectedSubUnit = code
                                            val curNum = qtyText.toDoubleOrNull() ?: 0.0
                                            if (code == "g" && curNum < 50.0) {
                                                qtyText = kotlin.math.round(curNum * 1000.0).toInt().toString()
                                            } else if (code == "kg" && curNum >= 100.0) {
                                                val converted = curNum / 1000.0
                                                qtyText = if (converted % 1.0 == 0.0) converted.toInt().toString() else converted.toString()
                                            }
                                        }
                                    },
                                    label = { Text(label, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandNavy,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else if (category == UnitCategory.LIQUID) {
                        Text("Unit Type:", style = MaterialTheme.typography.labelMedium, color = Color(0xFF475569))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("l" to "Litres (L)", "ml" to "Millilitres (ml)").forEach { (code, label) ->
                                val isSel = activeSubUnit == code
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        if (activeSubUnit != code) {
                                            selectedSubUnit = code
                                            val curNum = qtyText.toDoubleOrNull() ?: 0.0
                                            if (code == "ml" && curNum < 50.0) {
                                                qtyText = kotlin.math.round(curNum * 1000.0).toInt().toString()
                                            } else if (code == "l" && curNum >= 100.0) {
                                                val converted = curNum / 1000.0
                                                qtyText = if (converted % 1.0 == 0.0) converted.toInt().toString() else converted.toString()
                                            }
                                        }
                                    },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandNavy,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Quick Preset Chips
                    Text("Quick Presets:", style = MaterialTheme.typography.labelMedium, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuantityUtils.getPresetChips(item.unit).forEach { chip ->
                            SuggestionChip(
                                onClick = {
                                    if (category == UnitCategory.WEIGHT) {
                                        if (chip.qtyInBaseUnit < 1.0) {
                                            selectedSubUnit = "g"
                                            qtyText = kotlin.math.round(chip.qtyInBaseUnit * 1000.0).toInt().toString()
                                        } else {
                                            selectedSubUnit = "kg"
                                            qtyText = chip.qtyInBaseUnit.toInt().toString()
                                        }
                                    } else if (category == UnitCategory.LIQUID) {
                                        if (chip.qtyInBaseUnit < 1.0) {
                                            selectedSubUnit = "ml"
                                            qtyText = kotlin.math.round(chip.qtyInBaseUnit * 1000.0).toInt().toString()
                                        } else {
                                            selectedSubUnit = "l"
                                            qtyText = chip.qtyInBaseUnit.toInt().toString()
                                        }
                                    } else {
                                        qtyText = if (chip.qtyInBaseUnit % 1.0 == 0.0) chip.qtyInBaseUnit.toInt().toString() else chip.qtyInBaseUnit.toString()
                                    }
                                },
                                label = { Text(chip.label, fontSize = 12.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("Quantity (e.g. 500g, 250, 1.5)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_item_qty_input"),
                        singleLine = true,
                        colors = dukanTextFieldColors(),
                        textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Selling Price per ${item.unit} (₹)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_item_price_input"),
                        singleLine = true,
                        colors = dukanTextFieldColors(),
                        textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Live preview total
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${QuantityUtils.formatSmartQuantity(calculatedQty, item.unit)} @ ₹${calculatedPrice}/${item.unit}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "Line Total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Text(
                                text = formatRupee(calculatedTotal),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { selectedStockItem = null }) {
                            Text("Back")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (calculatedQty > 0) {
                                    onItemAdded(item, calculatedQty, calculatedPrice)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                            modifier = Modifier.testTag("confirm_add_item_button")
                        ) {
                            Text("Add to Bill")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerSelectDialog(
    customerList: List<Customer>,
    currentSelected: Customer?,
    onDismiss: () -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onSelectWalkIn: () -> Unit,
    onAddCustomer: (String, String) -> Unit
) {
    var isAddingNew by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }

    val filtered = customerList.filter { it.name.contains(search, ignoreCase = true) || it.phone.contains(search) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isAddingNew) LocaleStrings.get("add_customer") else LocaleStrings.get("select_customer"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandNavy
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (isAddingNew) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Customer Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = dukanTextFieldColors(),
                        textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Phone Number (WhatsApp)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = dukanTextFieldColors(),
                        textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isAddingNew = false }) { Text("Back") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    onAddCustomer(newName, newPhone)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                        ) {
                            Text("Save & Select")
                        }
                    }
                } else {
                    // Walk-in button
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectWalkIn() },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PersonOutline, contentDescription = null, tint = BrandNavy)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(LocaleStrings.get("walk_in"), fontWeight = FontWeight.Bold, color = BrandNavy)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Search customers...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = dukanTextFieldColors(),
                        textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Medium)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filtered) { customer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .clickable { onSelectCustomer(customer) }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(customer.name, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    if (customer.phone.isNotBlank()) {
                                        Text(customer.phone, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                    }
                                }
                                if (customer.balanceDue > 0) {
                                    Text(
                                        text = "Due: ${formatRupee(customer.balanceDue)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DangerRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { isAddingNew = true }) {
                            Text("+ New Customer", fontWeight = FontWeight.Bold, color = BrandBlue)
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreditCustomerDialog(
    customerList: List<Customer>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String) -> Unit,
    onSelectExisting: (Customer) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var isNewCustomerMode by remember { mutableStateOf(customerList.isEmpty()) }

    val filtered = if (searchQuery.isBlank()) customerList else customerList.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarningAmberBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Customer for Credit Sale",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )
                    Text(
                        text = "उधार / खाता ग्राहक चुनें",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = Color(0xFFFFF8E1),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Credit sales must be linked to a customer name so their Khata balance is accurately recorded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF856404),
                        modifier = Modifier.padding(10.dp)
                    )
                }

                if (customerList.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (!isNewCustomerMode) BrandNavy else Color.Transparent)
                                .clickable { isNewCustomerMode = false }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Existing (${customerList.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isNewCustomerMode) Color.White else Color.DarkGray
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isNewCustomerMode) BrandNavy else Color.Transparent)
                                .clickable { isNewCustomerMode = true }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "+ New Customer",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNewCustomerMode) Color.White else Color.DarkGray
                            )
                        }
                    }
                }

                if (isNewCustomerMode) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Customer Name *") },
                        placeholder = { Text("e.g. Ramesh Kumar") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("credit_customer_name_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number (Optional)") },
                        placeholder = { Text("e.g. 9876543210") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("credit_customer_phone_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                } else {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search name or phone...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filtered.forEach { cust ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectExisting(cust) },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        if (cust.phone.isNotBlank()) {
                                            Text(cust.phone, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                    if (cust.balanceDue > 0) {
                                        Text(
                                            "Due: ${formatRupee(cust.balanceDue)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DangerRed
                                        )
                                    }
                                }
                            }
                        }
                        if (filtered.isEmpty()) {
                            Text(
                                "No customer found. Switch to '+ New Customer' above.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isNewCustomerMode) {
                Button(
                    onClick = {
                        if (name.trim().isNotBlank()) {
                            onConfirm(name.trim(), phone.trim())
                        }
                    },
                    enabled = name.trim().isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_credit_customer_button")
                ) {
                    Text("Apply Credit")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

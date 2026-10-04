package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.model.Customer
import com.example.data.model.KhataEntry
import com.example.ui.strings.LocaleStrings
import com.example.ui.theme.dukanTextFieldColors
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ShopViewModel
import java.net.URLEncoder

fun sendWhatsAppReminder(context: Context, customer: Customer, shopName: String, lang: String) {
    try {
        val cleanPhone = customer.phone.replace(Regex("[^0-9]"), "")
        val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
        val template = LocaleStrings.get("reminder_msg", lang)
        val text = template.format(customer.name, shopName, customer.balanceDue)
        val encodedText = URLEncoder.encode(text, "UTF-8")

        val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedText"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Could not open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang by viewModel.language.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()
    val customers by viewModel.allCustomers.collectAsState()
    val totalDues by viewModel.totalDues.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddCustomerDialog by remember { mutableStateOf(false) }

    val shopTitle = shopProfile?.shopName?.ifEmpty { "Our Store" } ?: "Our Store"
    val filtered = customers
        .filter { it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery) }
        .sortedByDescending { it.balanceDue }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = LocaleStrings.get("khata_ledger", lang),
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddCustomerDialog = true },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White) },
                text = { Text(LocaleStrings.get("add_customer", lang), color = Color.White, fontWeight = FontWeight.Bold) },
                containerColor = BrandNavy,
                modifier = Modifier.testTag("fab_add_customer")
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandSurface)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp)
        ) {
            // Total Outstanding Dues Header Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = LocaleStrings.get("market_dues", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Text(
                                text = formatRupee(totalDues ?: 0.0),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = DangerRed
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(WarningAmberBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(LocaleStrings.get("search_customers", lang)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("khata_search_input"),
                    singleLine = true,
                    colors = dukanTextFieldColors(),
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Medium),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Customers List
            if (filtered.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No customers found", color = Color.Gray)
                        }
                    }
                }
            } else {
                items(filtered) { customer ->
                    CustomerKhataRowCard(
                        customer = customer,
                        lang = lang,
                        onClick = { viewModel.selectCustomer(customer.id) },
                        onSendReminder = {
                            sendWhatsAppReminder(context, customer, shopTitle, lang)
                        }
                    )
                }
            }
        }
    }

    if (showAddCustomerDialog) {
        AddCustomerDialog(
            lang = lang,
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { name, phone, initialDue ->
                viewModel.addCustomer(name, phone, initialDue)
                showAddCustomerDialog = false
            }
        )
    }
}

@Composable
fun CustomerKhataRowCard(
    customer: Customer,
    lang: String = "en",
    onClick: () -> Unit,
    onSendReminder: () -> Unit
) {
    val hasDues = customer.balanceDue > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("customer_card_${customer.id.take(6)}"),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (hasDues) DangerRedBg else SuccessGreenBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = customer.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = if (hasDues) DangerRed else SuccessGreen,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = customer.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.Black
                    )
                    if (customer.phone.isNotBlank()) {
                        Text(
                            text = customer.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatRupee(customer.balanceDue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hasDues) DangerRed else SuccessGreen
                    )
                    Text(
                        text = if (hasDues) LocaleStrings.get("due", lang) else LocaleStrings.get("settled", lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasDues) DangerRed else SuccessGreen
                    )
                }

                if (hasDues && customer.phone.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onSendReminder,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F5E9))
                            .testTag("reminder_btn_${customer.id.take(6)}")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send Reminder", tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataDetailScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang by viewModel.language.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()
    val customers by viewModel.allCustomers.collectAsState()
    val selectedId by viewModel.selectedCustomerId.collectAsState()
    val entries by viewModel.selectedCustomerEntries.collectAsState()

    val customer = customers.firstOrNull { it.id == selectedId }

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showCreditDialog by remember { mutableStateOf(false) }

    if (customer == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Customer not found")
        }
        return
    }

    val shopTitle = shopProfile?.shopName?.ifEmpty { "Our Store" } ?: "Our Store"
    val hasDues = customer.balanceDue > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = customer.name,
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.KHATA) },
                        modifier = Modifier.testTag("khata_detail_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = BrandNavy)
                    }
                },
                actions = {
                    if (customer.phone.isNotBlank() && hasDues) {
                        IconButton(onClick = { sendWhatsAppReminder(context, customer, shopTitle, lang) }) {
                            Icon(Icons.Default.Send, contentDescription = "Reminder", tint = Color(0xFF25D366))
                        }
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Payment Received (In)
                    Button(
                        onClick = { showPaymentDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("record_payment_btn")
                    ) {
                        Icon(Icons.Default.CallReceived, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(LocaleStrings.get("record_payment", lang), fontWeight = FontWeight.Bold)
                    }

                    // Credit Given (Out)
                    Button(
                        onClick = { showCreditDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("record_credit_btn")
                    ) {
                        Icon(Icons.Default.CallMade, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(LocaleStrings.get("record_credit", lang), fontWeight = FontWeight.Bold)
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
            // Customer Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                Text(
                                    text = customer.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                if (customer.phone.isNotBlank()) {
                                    Text(
                                        text = customer.phone,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.DarkGray
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = LocaleStrings.get("balance_due", lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                                Text(
                                    text = formatRupee(customer.balanceDue),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasDues) DangerRed else SuccessGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Create Bill for this Customer
                        OutlinedButton(
                            onClick = { viewModel.startNewBill(preselectedCustomer = customer) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = BrandNavy)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Bill for ${customer.name}", color = BrandNavy, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Entries List
            item {
                Text(
                    text = "${LocaleStrings.get("khata_entries", lang)} (${entries.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            if (entries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No transactions yet", color = Color.Gray)
                        }
                    }
                }
            } else {
                items(entries) { entry ->
                    KhataEntryCard(entry = entry)
                }
            }
        }
    }

    if (showPaymentDialog) {
        RecordEntryDialog(
            title = LocaleStrings.get("record_payment", lang),
            confirmButtonColor = SuccessGreen,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amount, note ->
                viewModel.recordKhataPayment(customer.id, amount, note)
                showPaymentDialog = false
            }
        )
    }

    if (showCreditDialog) {
        RecordEntryDialog(
            title = LocaleStrings.get("record_credit", lang),
            confirmButtonColor = DangerRed,
            onDismiss = { showCreditDialog = false },
            onConfirm = { amount, note ->
                viewModel.recordKhataCredit(customer.id, amount, note)
                showCreditDialog = false
            }
        )
    }
}

@Composable
fun KhataEntryCard(entry: KhataEntry) {
    val isPayment = entry.entryType == "payment_received"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isPayment) SuccessGreenBg else DangerRedBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPayment) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isPayment) SuccessGreen else DangerRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isPayment) "Payment Received" else "Credit / Bill",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    if (entry.note.isNotBlank()) {
                        Text(text = entry.note, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                    }
                    Text(text = formatDate(entry.entryDate), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }

            Text(
                text = (if (isPayment) "- " else "+ ") + formatRupee(entry.amount),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (isPayment) SuccessGreen else DangerRed
            )
        }
    }
}

@Composable
fun AddCustomerDialog(
    lang: String = "en",
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var openingDue by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = LocaleStrings.get("add_customer", lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandNavy
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cust_name_input"),
                    singleLine = true,
                    colors = dukanTextFieldColors(),
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (for WhatsApp reminder)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cust_phone_input"),
                    singleLine = true,
                    colors = dukanTextFieldColors(),
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Medium),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = openingDue,
                    onValueChange = { openingDue = it },
                    label = { Text("Opening Due Amount (₹) Optional") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = dukanTextFieldColors(),
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val due = openingDue.toDoubleOrNull() ?: 0.0
                                onConfirm(name, phone, due)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                        modifier = Modifier.testTag("confirm_add_cust_button")
                    ) {
                        Text("Add Customer")
                    }
                }
            }
        }
    }
}

@Composable
fun RecordEntryDialog(
    title: String,
    confirmButtonColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandNavy
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹) *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("entry_amount_input"),
                    singleLine = true,
                    colors = dukanTextFieldColors(),
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note / Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = dukanTextFieldColors(),
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Medium)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) onConfirm(amt, noteText)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = confirmButtonColor),
                        modifier = Modifier.testTag("confirm_entry_button")
                    ) {
                        Text("Save Entry")
                    }
                }
            }
        }
    }
}

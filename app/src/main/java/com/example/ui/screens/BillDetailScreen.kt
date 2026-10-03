package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
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
import com.example.data.model.BillItem
import com.example.pdf.InvoicePdfGenerator
import com.example.ui.strings.LocaleStrings
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ShopViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang by viewModel.language.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()
    val bill by viewModel.selectedBill.collectAsState()
    val items by viewModel.selectedBillItems.collectAsState()

    var showVoidDialog by remember { mutableStateOf(false) }

    if (bill == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No bill selected")
        }
        return
    }

    val currentBill = bill!!
    val isVoided = currentBill.status == "voided"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bill #${currentBill.billNumber.ifEmpty { currentBill.id.take(8).uppercase() }}",
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.HOME) },
                        modifier = Modifier.testTag("bill_detail_back_button")
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Share on WhatsApp
                        Button(
                            onClick = {
                                val pdf = InvoicePdfGenerator.generateBillPdf(context, shopProfile, currentBill, items)
                                if (pdf != null) {
                                    val shopName = shopProfile?.shopName?.ifEmpty { "Our Store" } ?: "Our Store"
                                    val msg = "Namaste! Here is your bill #${currentBill.billNumber} from $shopName. Total: ${formatRupee(currentBill.total)}. Thank you!"
                                    InvoicePdfGenerator.sharePdfViaWhatsApp(context, pdf, currentBill.customerPhoneSnapshot, msg)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), // WhatsApp Green
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("share_whatsapp_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(LocaleStrings.get("share_whatsapp", lang), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // Print / Open PDF
                        OutlinedButton(
                            onClick = {
                                val pdf = InvoicePdfGenerator.generateBillPdf(context, shopProfile, currentBill, items)
                                if (pdf != null) {
                                    InvoicePdfGenerator.viewOrPrintPdf(context, pdf)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("print_pdf_button")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, tint = BrandNavy)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(LocaleStrings.get("print_save_pdf", lang), color = BrandNavy, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    if (!isVoided) {
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(
                            onClick = { showVoidDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("void_bill_button")
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, tint = DangerRed)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(LocaleStrings.get("void_bill", lang), color = DangerRed, fontWeight = FontWeight.Bold)
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
            // Bill Status Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isVoided) DangerRedBg else Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = currentBill.billType.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatDate(currentBill.billDate),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.DarkGray
                                )
                            }
                            if (isVoided) {
                                StatusBadge(text = "VOIDED / निरस्त", bgColor = DangerRed, textColor = Color.White)
                            } else {
                                StatusBadge(
                                    text = currentBill.paymentMode.uppercase(),
                                    bgColor = SuccessGreenBg,
                                    textColor = SuccessGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        // Customer Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Customer:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(text = currentBill.customerNameSnapshot, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                            if (currentBill.customerPhoneSnapshot.isNotBlank()) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Phone:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(text = currentBill.customerPhoneSnapshot, fontWeight = FontWeight.Medium, color = Color.DarkGray)
                                }
                            }
                        }
                    }
                }
            }

            // Items List
            item {
                Text(
                    text = "Billed Items (${items.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            items(items) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.itemNameSnapshot, fontWeight = FontWeight.Bold, color = Color.Black)
                            val qtyStr = if (item.qty % 1.0 == 0.0) item.qty.toInt().toString() else "%.2f".format(item.qty)
                            Text(
                                text = "$qtyStr ${item.unit} × ₹${item.pricePerUnit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Text(
                            text = formatRupee(item.lineTotal),
                            fontWeight = FontWeight.Bold,
                            color = BrandNavy,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            // Financial Summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(LocaleStrings.get("subtotal", lang), color = Color.DarkGray)
                            Text(formatRupee(currentBill.subtotal), fontWeight = FontWeight.Medium)
                        }

                        if (currentBill.discount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(LocaleStrings.get("discount", lang), color = DangerRed)
                                Text("-${formatRupee(currentBill.discount)}", color = DangerRed, fontWeight = FontWeight.Medium)
                            }
                        }

                        if (currentBill.gstAmount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("GST (${currentBill.gstRatePercent}%)", color = Color.DarkGray)
                                Text(formatRupee(currentBill.gstAmount), fontWeight = FontWeight.Medium)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = LocaleStrings.get("total_payable", lang),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy
                            )
                            Text(
                                text = formatRupee(currentBill.total),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy
                            )
                        }
                    }
                }
            }
        }
    }

    // Void Bill Confirmation Dialog
    if (showVoidDialog) {
        AlertDialog(
            onDismissRequest = { showVoidDialog = false },
            title = {
                Text(LocaleStrings.get("void_bill", lang), fontWeight = FontWeight.Bold, color = DangerRed)
            },
            text = {
                Text(LocaleStrings.get("void_bill_confirm", lang))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showVoidDialog = false
                        viewModel.voidCurrentBill()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Yes, Void Bill / निरस्त करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoidDialog = false }) {
                    Text(LocaleStrings.get("cancel", lang))
                }
            }
        )
    }
}

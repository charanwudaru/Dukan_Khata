package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Bill
import com.example.sync.SyncState
import com.example.ui.strings.LocaleStrings
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val shopProfile by viewModel.shopProfile.collectAsState()
    val lang by viewModel.language.collectAsState()
    val todaySales by viewModel.todaySales.collectAsState()
    val lowStockCount by viewModel.lowStockCount.collectAsState()
    val totalDues by viewModel.totalDues.collectAsState()
    val recentBills by viewModel.allBills.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = shopProfile?.shopName?.ifEmpty { LocaleStrings.get("app_name", lang) } ?: LocaleStrings.get("app_name", lang),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = BrandNavy
                        )
                        Text(
                            text = shopProfile?.ownerName?.ifEmpty { LocaleStrings.get("tagline", lang) } ?: LocaleStrings.get("tagline", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    // Sync indicator badge
                    val (syncText, syncBg, syncFg) = when {
                        !syncStatus.isConnected -> Triple("Backup Off", Color(0xFFEEEEEE), Color.Gray)
                        !syncStatus.isOnline -> Triple("Offline", DangerRedBg, DangerRed)
                        syncStatus.syncState == SyncState.SYNCING -> Triple("Syncing...", WarningAmberBg, WarningAmber)
                        syncStatus.pendingCount > 0 -> Triple("${syncStatus.pendingCount} queued", WarningAmberBg, WarningAmber)
                        else -> Triple("Synced ✓", SuccessGreenBg, SuccessGreen)
                    }

                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(syncBg)
                            .clickable { viewModel.navigateTo(AppScreen.SETTINGS) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = syncText, color = syncFg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Language switch button
                    IconButton(
                        onClick = {
                            viewModel.setLanguage(if (lang == "hi") "en" else "hi")
                        },
                        modifier = Modifier.testTag("lang_toggle_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE8EAF6),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (lang == "hi") "अ" else "EN",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BrandNavy
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.startNewBill() },
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) },
                text = { Text(LocaleStrings.get("new_bill", lang), color = Color.White, fontWeight = FontWeight.Bold) },
                containerColor = BrandNavy,
                modifier = Modifier.testTag("fab_new_bill")
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
        ) {
            // Section: 3 Critical Numbers
            item {
                Text(
                    text = "Shop Overview / दुकान स्थिति",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            // 1. Today's Sales
            item {
                val cashText = "Cash: ${formatRupee(todaySales.cashSales)}"
                val upiText = "UPI: ${formatRupee(todaySales.upiSales)}"
                val creditText = "Credit: ${formatRupee(todaySales.creditSales)}"
                MetricStatCard(
                    title = LocaleStrings.get("today_sales", lang),
                    value = formatRupee(todaySales.totalSales),
                    subtitle = "$cashText • $upiText • $creditText (${todaySales.billCount} bills)",
                    icon = Icons.Default.CurrencyRupee,
                    iconBgColor = Color(0xFFE3F2FD),
                    iconColor = BrandBlue,
                    onClick = { viewModel.navigateTo(AppScreen.REPORTS) },
                    modifier = Modifier.testTag("card_today_sales")
                )
            }

            // 2 & 3: Low Stock & Pending Dues in Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Low Stock Alert Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.navigateTo(AppScreen.STOCK) }
                            .testTag("card_low_stock"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (lowStockCount > 0) DangerRedBg else Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (lowStockCount > 0) DangerRed else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                if (lowStockCount > 0) {
                                    StatusBadge(text = "ALERT", bgColor = DangerRedBg, textColor = DangerRed)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = LocaleStrings.get("low_stock_alerts", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray
                            )
                            Text(
                                text = "$lowStockCount items",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (lowStockCount > 0) DangerRed else Color.Black
                            )
                        }
                    }

                    // Pending Dues Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.navigateTo(AppScreen.KHATA) }
                            .testTag("card_pending_dues"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
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
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = LocaleStrings.get("pending_dues", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray
                            )
                            Text(
                                text = formatRupee(totalDues ?: 0.0),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if ((totalDues ?: 0.0) > 0) DangerRed else SuccessGreen
                            )
                        }
                    }
                }
            }

            // Quick Actions Bar
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = LocaleStrings.get("quick_actions", lang),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            QuickActionButton(
                                icon = Icons.Default.ReceiptLong,
                                label = LocaleStrings.get("new_bill", lang),
                                bgColor = Color(0xFFE8EAF6),
                                iconColor = BrandNavy,
                                onClick = { viewModel.startNewBill() },
                                testTag = "quick_new_bill"
                            )
                            QuickActionButton(
                                icon = Icons.Default.AddBox,
                                label = LocaleStrings.get("add_item", lang),
                                bgColor = Color(0xFFE8F5E9),
                                iconColor = SuccessGreen,
                                onClick = { viewModel.navigateTo(AppScreen.STOCK) },
                                testTag = "quick_add_stock"
                            )
                            QuickActionButton(
                                icon = Icons.Default.Payments,
                                label = LocaleStrings.get("receive_payment", lang),
                                bgColor = Color(0xFFFFF3E0),
                                iconColor = WarningAmber,
                                onClick = { viewModel.navigateTo(AppScreen.KHATA) },
                                testTag = "quick_payment"
                            )
                            QuickActionButton(
                                icon = Icons.Default.CloudSync,
                                label = "Sync / बैकअप",
                                bgColor = Color(0xFFF3E5F5),
                                iconColor = Color(0xFF7B1FA2),
                                onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                                testTag = "quick_sync"
                            )
                        }
                    }
                }
            }

            // Section: Recent Bills
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = LocaleStrings.get("recent_bills", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    if (recentBills.isNotEmpty()) {
                        Text(
                            text = LocaleStrings.get("view_all", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.REPORTS) }
                        )
                    }
                }
            }

            if (recentBills.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = LocaleStrings.get("no_bills_yet", lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.startNewBill() },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                            ) {
                                Text(LocaleStrings.get("new_bill", lang))
                            }
                        }
                    }
                }
            } else {
                items(recentBills.take(8)) { bill ->
                    BillItemCard(
                        bill = bill,
                        onClick = { viewModel.openBillDetail(bill) }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    bgColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .testTag(testTag)
            .width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(26.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 13.sp
        )
    }
}

@Composable
fun BillItemCard(
    bill: Bill,
    onClick: () -> Unit
) {
    val isVoided = bill.status == "voided"
    val isQuotation = bill.billType == "quotation"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("bill_card_${bill.id.take(6)}"),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = bill.billNumber.ifEmpty { bill.id.take(8).uppercase() },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isVoided) Color.Gray else Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isVoided) {
                        StatusBadge(text = "VOIDED", bgColor = DangerRedBg, textColor = DangerRed)
                    } else if (isQuotation) {
                        StatusBadge(text = "ESTIMATE", bgColor = Color(0xFFEDE7F6), textColor = Color(0xFF512DA8))
                    } else {
                        val modeColor = when (bill.paymentMode.lowercase()) {
                            "cash" -> SuccessGreen
                            "upi" -> BrandBlue
                            else -> WarningAmber
                        }
                        val modeBg = when (bill.paymentMode.lowercase()) {
                            "cash" -> SuccessGreenBg
                            "upi" -> Color(0xFFE3F2FD)
                            else -> WarningAmberBg
                        }
                        StatusBadge(text = bill.paymentMode.uppercase(), bgColor = modeBg, textColor = modeColor)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bill.customerNameSnapshot,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )
                Text(
                    text = formatDate(bill.billDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatRupee(bill.total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isVoided) Color.Gray else BrandNavy
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

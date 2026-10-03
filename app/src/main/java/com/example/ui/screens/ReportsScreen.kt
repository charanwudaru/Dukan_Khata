package com.example.ui.screens

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
import com.example.ui.strings.LocaleStrings
import com.example.ui.viewmodel.ShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang by viewModel.language.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()
    val todaySales by viewModel.todaySales.collectAsState()
    val monthlySales by viewModel.monthlySales.collectAsState()
    val profitReport by viewModel.profitReport.collectAsState()
    val stockValuation by viewModel.stockValuation.collectAsState()
    val topItems by viewModel.topSellingItems.collectAsState()
    val customers by viewModel.allCustomers.collectAsState()

    val shopTitle = shopProfile?.shopName?.ifEmpty { "Our Store" } ?: "Our Store"
    val customersWithDues = customers.filter { it.balanceDue > 0 }.sortedByDescending { it.balanceDue }

    LaunchedEffect(Unit) {
        viewModel.loadReports()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = LocaleStrings.get("reports_analytics", lang),
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
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
                                text = "Gross Profit / कुल मुनाफ़ा",
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

            // Top Selling Items
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

            // Outstanding Dues List
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
}

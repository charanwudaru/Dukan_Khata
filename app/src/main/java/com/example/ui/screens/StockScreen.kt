package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.StockItem
import com.example.ui.strings.LocaleStrings
import com.example.ui.theme.dukanTextFieldColors
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.language.collectAsState()
    val allStock by viewModel.allStock.collectAsState()
    val lowStockCount by viewModel.lowStockCount.collectAsState()
    val stockValuation by viewModel.stockValuation.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterLowStockOnly by remember { mutableStateOf(false) }

    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemToRestock by remember { mutableStateOf<StockItem?>(null) }
    var itemToAdjust by remember { mutableStateOf<StockItem?>(null) }
    var itemToEdit by remember { mutableStateOf<StockItem?>(null) }
    var itemToArchive by remember { mutableStateOf<StockItem?>(null) }

    val filtered = allStock.filter { item ->
        val matchesSearch = item.name.contains(searchQuery, ignoreCase = true)
        val matchesFilter = if (filterLowStockOnly) item.currentQty <= item.lowStockThreshold else true
        matchesSearch && matchesFilter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = LocaleStrings.get("stock_inventory", lang),
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddItemDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) },
                text = { Text(LocaleStrings.get("add_stock_item", lang), color = Color.White, fontWeight = FontWeight.Bold) },
                containerColor = BrandNavy,
                modifier = Modifier.testTag("fab_add_stock_item")
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
            // Valuation & Count Header Card
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
                                text = LocaleStrings.get("valuation", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Text(
                                text = formatRupee(stockValuation),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total Items",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Text(
                                text = "${allStock.size} items",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }

            // Search Bar & Filter Chip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(LocaleStrings.get("search_stock", lang)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stock_search_input"),
                        singleLine = true,
                        colors = dukanTextFieldColors(),
                        textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Medium),
                        shape = RoundedCornerShape(12.dp)
                    )

                    FilterChip(
                        selected = filterLowStockOnly,
                        onClick = { filterLowStockOnly = !filterLowStockOnly },
                        label = { Text("Low Stock ($lowStockCount)") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (filterLowStockOnly) Color.White else DangerRed
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DangerRed,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_low_stock_chip")
                    )
                }
            }

            // Items List
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
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No items found", color = Color.Gray)
                        }
                    }
                }
            } else {
                items(filtered) { item ->
                    StockItemRowCard(
                        item = item,
                        onRestock = { itemToRestock = item },
                        onAdjust = { itemToAdjust = item },
                        onEdit = { itemToEdit = item },
                        onArchive = { itemToArchive = item }
                    )
                }
            }
        }
    }

    // Add Item Dialog
    if (showAddItemDialog) {
        AddOrEditStockItemDialog(
            existingItem = null,
            onDismiss = { showAddItemDialog = false },
            onSave = { name, unit, openingQty, pPrice, sPrice, threshold ->
                viewModel.addStockItem(name, unit, openingQty, pPrice, sPrice, threshold)
                showAddItemDialog = false
            }
        )
    }

    // Edit Item Dialog
    if (itemToEdit != null) {
        AddOrEditStockItemDialog(
            existingItem = itemToEdit,
            onDismiss = { itemToEdit = null },
            onSave = { name, unit, openingQty, pPrice, sPrice, threshold ->
                viewModel.updateStockItem(
                    itemToEdit!!.copy(
                        name = name,
                        unit = unit,
                        purchasePrice = pPrice,
                        sellingPrice = sPrice,
                        lowStockThreshold = threshold
                    )
                )
                itemToEdit = null
            }
        )
    }

    // Restock Dialog
    if (itemToRestock != null) {
        RestockDialog(
            item = itemToRestock!!,
            onDismiss = { itemToRestock = null },
            onConfirm = { qty, price, supplier ->
                viewModel.restockItem(itemToRestock!!.id, qty, price, supplier)
                itemToRestock = null
            }
        )
    }

    // Adjust Stock Dialog
    if (itemToAdjust != null) {
        AdjustStockDialog(
            item = itemToAdjust!!,
            onDismiss = { itemToAdjust = null },
            onConfirm = { qtyChange, reason ->
                viewModel.adjustStock(itemToAdjust!!.id, qtyChange, reason)
                itemToAdjust = null
            }
        )
    }

    // Archive Confirmation Dialog
    if (itemToArchive != null) {
        AlertDialog(
            onDismissRequest = { itemToArchive = null },
            title = { Text("Archive '${itemToArchive?.name}'?") },
            text = { Text("This item will be removed from your active stock list, but historical bills will remain intact.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.archiveStockItem(itemToArchive!!.id)
                        itemToArchive = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Archive")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToArchive = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun StockItemRowCard(
    item: StockItem,
    onRestock: () -> Unit,
    onAdjust: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit
) {
    val isLow = item.currentQty <= item.lowStockThreshold

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stock_item_${item.id.take(6)}"),
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
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                        if (isLow) {
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusBadge(text = "LOW STOCK", bgColor = DangerRedBg, textColor = DangerRed)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cost: ₹${item.purchasePrice} • Sell: ₹${item.sellingPrice}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                }

                // Current Qty Display
                val qtyStr = if (item.currentQty % 1.0 == 0.0) item.currentQty.toInt().toString() else "%.2f".format(item.currentQty)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$qtyStr ${item.unit}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isLow) DangerRed else SuccessGreen
                    )
                    Text(
                        text = "Threshold: ${item.lowStockThreshold} ${item.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onAdjust) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Adjust", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(4.dp))

                FilledTonalButton(
                    onClick = onRestock,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFE8EAF6))
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = BrandNavy, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restock", color = BrandNavy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }

                IconButton(onClick = onArchive, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Archive, contentDescription = "Archive", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun AddOrEditStockItemDialog(
    existingItem: StockItem?,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Double, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf(existingItem?.name ?: "") }
    var unit by remember { mutableStateOf(existingItem?.unit ?: "pc") }
    var openingQty by remember { mutableStateOf(existingItem?.currentQty?.toString() ?: "10") }
    var purchasePrice by remember { mutableStateOf(existingItem?.purchasePrice?.toString() ?: "0") }
    var sellingPrice by remember { mutableStateOf(existingItem?.sellingPrice?.toString() ?: "0") }
    var lowStockThreshold by remember { mutableStateOf(existingItem?.lowStockThreshold?.toString() ?: "5") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = if (existingItem == null) "Add Stock Item / नया सामान" else "Edit Stock Item",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandNavy
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name (e.g. Basmati Rice, Milk)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stock_name_input"),
                    singleLine = true,
                    colors = dukanTextFieldColors(),
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (kg, litre, pc)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = dukanTextFieldColors(),
                        textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Medium)
                    )
                    OutlinedTextField(
                        value = openingQty,
                        onValueChange = { openingQty = it },
                        label = { Text("Quantity") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = dukanTextFieldColors(),
                        textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = purchasePrice,
                        onValueChange = { purchasePrice = it },
                        label = { Text("Purchase Price (₹)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = dukanTextFieldColors(),
                        textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Medium),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = sellingPrice,
                        onValueChange = { sellingPrice = it },
                        label = { Text("Selling Price (₹)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stock_selling_price_input"),
                        singleLine = true,
                        colors = dukanTextFieldColors(),
                        textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = lowStockThreshold,
                    onValueChange = { lowStockThreshold = it },
                    label = { Text("Low Stock Alert Threshold") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = dukanTextFieldColors(),
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Medium),
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
                                onSave(
                                    name,
                                    unit,
                                    openingQty.toDoubleOrNull() ?: 0.0,
                                    purchasePrice.toDoubleOrNull() ?: 0.0,
                                    sellingPrice.toDoubleOrNull() ?: 0.0,
                                    lowStockThreshold.toDoubleOrNull() ?: 5.0
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                        modifier = Modifier.testTag("confirm_save_stock_button")
                    ) {
                        Text("Save Item")
                    }
                }
            }
        }
    }
}

@Composable
fun RestockDialog(
    item: StockItem,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double, String) -> Unit
) {
    var qtyText by remember { mutableStateOf("10") }
    var priceText by remember { mutableStateOf(item.purchasePrice.toString()) }
    var supplierText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Restock: ${item.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandNavy
                )
                Text(
                    text = "Current Stock: ${item.currentQty} ${item.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("Quantity Added (${item.unit})") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = dukanTextFieldColors(),
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Purchase Price per unit (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = dukanTextFieldColors(),
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = supplierText,
                    onValueChange = { supplierText = it },
                    label = { Text("Supplier Name (Optional)") },
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
                            val qty = qtyText.toDoubleOrNull() ?: 0.0
                            val price = priceText.toDoubleOrNull() ?: item.purchasePrice
                            if (qty > 0) onConfirm(qty, price, supplierText)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                    ) {
                        Text("Confirm Restock")
                    }
                }
            }
        }
    }
}

@Composable
fun AdjustStockDialog(
    item: StockItem,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var qtyChangeText by remember { mutableStateOf("-1") }
    var reason by remember { mutableStateOf("Damaged") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Adjust Quantity: ${item.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandNavy
                )
                Text(
                    text = "Current Stock: ${item.currentQty} ${item.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = qtyChangeText,
                    onValueChange = { qtyChangeText = it },
                    label = { Text("Quantity Change (+ or -)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = dukanTextFieldColors(),
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (Damage / Expired / Return)") },
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
                            val change = qtyChangeText.toDoubleOrNull() ?: 0.0
                            if (change != 0.0) onConfirm(change, reason)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
                    ) {
                        Text("Apply Adjustment")
                    }
                }
            }
        }
    }
}

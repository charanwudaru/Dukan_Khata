package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ShopProfile
import com.example.sync.SyncState
import com.example.ui.strings.LocaleStrings
import com.example.ui.theme.dukanTextFieldColors
import com.example.ui.viewmodel.ShopViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val lang by viewModel.language.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()

    var showProfileDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showScriptGuideDialog by remember { mutableStateOf(false) }
    var sheetInput by remember { mutableStateOf(syncStatus.sheetIdOrUrl) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = LocaleStrings.get("settings", lang),
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
            // Lifetime License Card (Play Store ₹49)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isPremium) SuccessGreenBg else Color(0xFFFFF8E1)),
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
                                    text = if (isPremium) LocaleStrings.get("license_active", lang) else LocaleStrings.get("premium_status", lang),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (isPremium) SuccessGreen else Color(0xFFE65100)
                                )
                                Text(
                                    text = if (isPremium) "100% Offline • Lifetime Unlimited Bills & Ledger" else "One-time purchase • ₹49 only • No recurring fees",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.DarkGray
                                )
                            }
                            Icon(
                                imageVector = if (isPremium) Icons.Default.CheckCircle else Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isPremium) SuccessGreen else Color(0xFFFFA000),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        if (!isPremium) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.purchasePremiumLicense() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                    modifier = Modifier.weight(1f).testTag("buy_license_btn")
                                ) {
                                    Text(LocaleStrings.get("upgrade_license", lang), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.restorePurchase() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(LocaleStrings.get("restore_purchase", lang), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Shop Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = LocaleStrings.get("shop_profile", lang),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = BrandNavy
                            )
                            IconButton(onClick = { showProfileDialog = true }, modifier = Modifier.testTag("edit_profile_btn")) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = BrandBlue)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(text = "Shop: ${shopProfile?.shopName?.ifEmpty { "Not configured" }}", fontWeight = FontWeight.Bold)
                        Text(text = "Owner: ${shopProfile?.ownerName?.ifEmpty { "Not configured" }}", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                        Text(text = "Phone: ${shopProfile?.phone?.ifEmpty { "Not configured" }}", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                        Text(text = "Address: ${shopProfile?.address?.ifEmpty { "Not configured" }}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(
                            text = "GSTIN: ${if (!shopProfile?.gstin.isNullOrBlank()) shopProfile?.gstin else "None (Regular retail bill)"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            // App Language Selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = LocaleStrings.get("language", lang),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = BrandNavy
                            )
                            val currentLangName = when (lang) {
                                "te" -> "తెలుగు (Telugu)"
                                "ta" -> "தமிழ் (Tamil)"
                                "kn" -> "ಕನ್ನಡ (Kannada)"
                                "ml" -> "മലയാളം (Malayalam)"
                                "mr" -> "मराठी (Marathi)"
                                "bn" -> "বাংলা (Bengali)"
                                "gu" -> "ગુજરાતી (Gujarati)"
                                "pa" -> "ਪੰਜਾਬੀ (Punjabi)"
                                "hi" -> "हिंदी (Hindi)"
                                else -> "English"
                            }
                            Text(
                                text = currentLangName,
                                style = MaterialTheme.typography.labelMedium,
                                color = BrandNavy,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Two column grid for easy touch on mobile
                        val chunked = listOf(
                            listOf("en" to "English", "hi" to "हिंदी (Hindi)"),
                            listOf("te" to "తెలుగు (Telugu)", "ta" to "தமிழ் (Tamil)"),
                            listOf("kn" to "ಕನ್ನಡ (Kannada)", "ml" to "മലയാളം (Malayalam)"),
                            listOf("mr" to "मराठी (Marathi)", "bn" to "বাংলা (Bengali)"),
                            listOf("gu" to "ગુજરાતી (Gujarati)", "pa" to "ਪੰਜਾਬੀ (Punjabi)")
                        )

                        chunked.forEach { rowLangs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowLangs.forEach { (code, label) ->
                                    val isSelected = lang == code
                                    OutlinedButton(
                                        onClick = { viewModel.setLanguage(code) },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isSelected) Color(0xFFE8EAF6) else Color.Transparent
                                        ),
                                        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("lang_${code}_btn")
                                    ) {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) BrandNavy else Color(0xFF1E293B),
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }

            // Google Sheets Backup & Restore Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = LocaleStrings.get("backup_sync", lang),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = BrandNavy
                            )
                            val (badgeText, badgeBg, badgeFg) = when {
                                !syncStatus.isConnected -> Triple("Disconnected", Color(0xFFEEEEEE), Color.Gray)
                                !syncStatus.isOnline -> Triple("Offline", DangerRedBg, DangerRed)
                                syncStatus.syncState == SyncState.SYNCING -> Triple("Syncing...", WarningAmberBg, WarningAmber)
                                syncStatus.pendingCount > 0 -> Triple("${syncStatus.pendingCount} Queued", WarningAmberBg, WarningAmber)
                                else -> Triple("Connected ✓", SuccessGreenBg, SuccessGreen)
                            }
                            StatusBadge(text = badgeText, bgColor = badgeBg, textColor = badgeFg)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (syncStatus.isConnected) {
                            if (syncStatus.isWebhook) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Bolt, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("🟢 Live Real-Time PC Sync Active", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1B5E20))
                                            Text("Every bill, stock change, and payment updates your Google Sheet on your PC immediately!", fontSize = 11.sp, color = Color(0xFF2E7D32))
                                        }
                                    }
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Why isn't my PC Sheet updating?", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFE65100))
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Google blocks external apps from directly modifying your sheet on PC without an Apps Script Web App. Deploy the 1-minute script to get live updates and dashboard charts.", fontSize = 11.sp, color = Color(0xFFBF360C))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { showScriptGuideDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Setup Real-Time PC Sync (1-Click Script)", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            val targetDesc = if (syncStatus.isWebhook) "Connected Webhook (Auto-writes to PC)" else "Sheet ID: ${syncStatus.sheetIdOrUrl.take(18)}..."
                            Text(text = targetDesc, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                            if (syncStatus.lastSyncTime > 0) {
                                Text(
                                    text = LocaleStrings.get("last_synced", lang).format(formatDate(syncStatus.lastSyncTime)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                            if (syncStatus.pendingCount > 0) {
                                Text(
                                    text = LocaleStrings.get("unsynced_items", lang).format(syncStatus.pendingCount),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WarningAmber
                                )
                            }

                            if (syncStatus.errorMessage != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = syncStatus.errorMessage!!, style = MaterialTheme.typography.bodySmall, color = DangerRed)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val success = viewModel.syncManager.syncNow()
                                            if (success) {
                                                Toast.makeText(context, "Google Sheet updated on your PC!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                                    modifier = Modifier.weight(1f).testTag("sync_now_btn")
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sync Now / Update PC")
                                }

                                OutlinedButton(
                                    onClick = { viewModel.syncManager.exportAndShareToGoogleSheets(context) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = BrandNavy)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(LocaleStrings.get("export_to_sheets", lang), color = BrandNavy, fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showScriptGuideDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Code, contentDescription = null, tint = BrandNavy, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("View Apps Script", color = BrandNavy, fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.syncManager.disconnectBackup() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(LocaleStrings.get("disconnect_backup", lang), color = DangerRed, fontSize = 12.sp)
                                }
                            }
                        } else {
                            Text(
                                text = "Keep your shop data safely backed up and updated live in your Google Sheet on your PC with 5 organized sheets & dashboard graphs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = sheetInput,
                                onValueChange = { sheetInput = it },
                                label = { Text("Apps Script Webhook URL or Sheet Link") },
                                placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                                modifier = Modifier.fillMaxWidth().testTag("sheet_url_input"),
                                singleLine = true,
                                colors = dukanTextFieldColors(),
                                textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Medium)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val res = viewModel.syncManager.connectBackupSheet(sheetInput)
                                            if (res.isSuccess) {
                                                Toast.makeText(context, "Connected successfully!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, res.exceptionOrNull()?.message ?: "Connection failed", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                                    modifier = Modifier.weight(1f).testTag("connect_sheet_btn")
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(LocaleStrings.get("connect_sheet", lang))
                                }

                                OutlinedButton(
                                    onClick = { showScriptGuideDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = BrandNavy)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Setup Guide & Script", fontSize = 11.sp, color = BrandNavy)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        // Manual Export / Import Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val csvData = viewModel.syncManager.generateExportCsvString()
                                        clipboardManager.setText(AnnotatedString(csvData))

                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "Dukan Khata Full Backup")
                                            putExtra(Intent.EXTRA_TEXT, csvData)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Export Backup to"))
                                        Toast.makeText(context, "Backup copied to clipboard and ready to share!", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("export_backup_btn")
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(LocaleStrings.get("export_csv", lang), fontSize = 12.sp)
                            }

                            Button(
                                onClick = { showRestoreDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                modifier = Modifier.weight(1f).testTag("restore_backup_btn")
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(LocaleStrings.get("import_backup", lang), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Profile Dialog
    if (showProfileDialog) {
        EditProfileDialog(
            profile = shopProfile ?: ShopProfile(),
            onDismiss = { showProfileDialog = false },
            onSave = { updated ->
                scope.launch {
                    viewModel.repository.saveProfile(updated)
                    showProfileDialog = false
                    Toast.makeText(context, "Profile saved!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Restore Backup Dialog
    if (showRestoreDialog) {
        RestoreBackupDialog(
            onDismiss = { showRestoreDialog = false },
            onRestore = { rawData ->
                scope.launch {
                    val result = viewModel.syncManager.restoreFromBackupData(rawData)
                    showRestoreDialog = false
                    if (result.isSuccess) {
                        val res = result.getOrNull()
                        viewModel.refreshDashboardMetrics()
                        Toast.makeText(context, "Restored ${res?.restoredCount} items (${res?.skippedCount} duplicates skipped)", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Restore failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Real-Time Sheet Guide Dialog
    if (showScriptGuideDialog) {
        RealtimeSheetGuideDialog(
            scriptCode = viewModel.syncManager.getSampleAppsScript(),
            onDismiss = { showScriptGuideDialog = false },
            onConnect = { url ->
                scope.launch {
                    val res = viewModel.syncManager.connectBackupSheet(url)
                    if (res.isSuccess) {
                        Toast.makeText(context, "Real-Time PC Sync connected! Google Sheet updated.", Toast.LENGTH_SHORT).show()
                        showScriptGuideDialog = false
                    } else {
                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Connection failed", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@Composable
fun EditProfileDialog(
    profile: ShopProfile,
    onDismiss: () -> Unit,
    onSave: (ShopProfile) -> Unit
) {
    var shopName by remember { mutableStateOf(profile.shopName) }
    var ownerName by remember { mutableStateOf(profile.ownerName) }
    var phone by remember { mutableStateOf(profile.phone) }
    var gstin by remember { mutableStateOf(profile.gstin ?: "") }
    var address by remember { mutableStateOf(profile.address) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = LocaleStrings.get("shop_profile"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandNavy
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("Shop Name") },
                    modifier = Modifier.fillMaxWidth().testTag("profile_shop_name_input"),
                    singleLine = true,
                    colors = dukanTextFieldColors(),
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Owner Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = dukanTextFieldColors(),
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Medium)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = dukanTextFieldColors(),
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Medium)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = gstin,
                    onValueChange = { gstin = it },
                    label = { Text("GSTIN (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = dukanTextFieldColors(),
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Medium)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Shop Address") },
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
                            onSave(
                                profile.copy(
                                    shopName = shopName.trim(),
                                    ownerName = ownerName.trim(),
                                    phone = phone.trim(),
                                    gstin = gstin.trim().uppercase(),
                                    address = address.trim()
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                        modifier = Modifier.testTag("save_profile_btn")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun RestoreBackupDialog(
    onDismiss: () -> Unit,
    onRestore: (String) -> Unit
) {
    var rawText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Import / Restore Backup Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandNavy
                )
                Text(
                    text = "Paste your exported CSV data below. Existing items are preserved, and new rows are inserted in safe dependency order.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = { Text("Paste CSV data here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("restore_raw_input"),
                    maxLines = 10,
                    colors = dukanTextFieldColors(),
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Normal)
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
                            if (rawText.isNotBlank()) onRestore(rawText)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                        modifier = Modifier.testTag("confirm_restore_btn")
                    ) {
                        Text("Restore Now")
                    }
                }
            }
        }
    }
}

@Composable
fun RealtimeSheetGuideDialog(
    scriptCode: String,
    onDismiss: () -> Unit,
    onConnect: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var webAppUrlInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ Real-Time PC Sync & Charts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Single Workbook with 5 Clean Sheets & Graphs:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1B5E20)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("1. 📊 Dashboard: Live KPI cards (Revenue, Cash, UPI, Udhar, Stock Value, Low Stock Alerts) + Payment Mode Pie Chart & Stock Column Chart!", fontSize = 11.sp, color = Color(0xFF2E7D32))
                        Text("2. 🧾 Bills: All invoice records, customer details, and item summaries", fontSize = 11.sp, color = Color(0xFF2E7D32))
                        Text("3. 📦 Stock: Live inventory levels, costs, valuations, and low-stock highlights", fontSize = 11.sp, color = Color(0xFF2E7D32))
                        Text("4. 👥 Khata: Customer ledger & outstanding dues", fontSize = 11.sp, color = Color(0xFF2E7D32))
                        Text("5. 💳 Transactions: Complete payment & credit history", fontSize = 11.sp, color = Color(0xFF2E7D32))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Why doesn't a direct sheet URL update in real-time?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.Black
                )
                Text(
                    text = "Google's security requires a 1-minute Apps Script Web App so your phone can securely write changes directly into your Google Sheet on your PC as you bill customers or adjust stock.",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "4 Easy Steps to Connect (Takes 1 min):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = BrandNavy
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text("1. Open your Google Sheet on your PC.", fontSize = 12.sp, color = Color(0xFF1E293B))
                Text("2. Click 'Extensions' > 'Apps Script' in the top menu.", fontSize = 12.sp, color = Color(0xFF1E293B))
                Text("3. Delete any default text, paste this script, and save.", fontSize = 12.sp, color = Color(0xFF1E293B))
                Text("4. Click 'Deploy' > 'New deployment' -> Select 'Web app' -> Set 'Who has access' to 'Anyone' -> Click Deploy. Copy the Web App URL and paste below!", fontSize = 12.sp, color = Color(0xFF1E293B))

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(scriptCode))
                            Toast.makeText(context, "Full Apps Script Copied to Clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Script", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Dukan Khata Google Sheets Sync Script")
                                putExtra(Intent.EXTRA_TEXT, scriptCode)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Script to PC via WhatsApp / Email"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = BrandNavy, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share to PC", fontSize = 12.sp, color = BrandNavy)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Paste Your Web App URL Here:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = BrandNavy
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = webAppUrlInput,
                    onValueChange = { webAppUrlInput = it },
                    placeholder = { Text("https://script.google.com/macros/s/.../exec") },
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
                            if (webAppUrlInput.isNotBlank()) {
                                onConnect(webAppUrlInput.trim())
                            } else {
                                Toast.makeText(context, "Please paste the Web App URL first", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connect & Live Sync")
                    }
                }
            }
        }
    }
}

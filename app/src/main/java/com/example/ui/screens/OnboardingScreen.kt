package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.strings.LocaleStrings
import com.example.ui.theme.dukanTextFieldColors
import com.example.ui.viewmodel.ShopViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lang by viewModel.language.collectAsState()

    var step by remember { mutableStateOf(1) } // 1: Shop details, 2: Start Fresh or Restore
    var shopName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    var backupSheetInput by remember { mutableStateOf("") }
    var showRestoreCsvDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = LocaleStrings.get("app_name", lang),
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.setLanguage(if (lang == "hi") "en" else "hi")
                        },
                        modifier = Modifier.testTag("onboarding_lang_toggle")
                    ) {
                        Text(
                            text = if (lang == "hi") "English" else "हिंदी",
                            fontWeight = FontWeight.Bold,
                            color = BrandNavy
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandSurface)
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8EAF6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = BrandNavy,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (step == 1) "Welcome to Dukan Khata!" else "Data Setup / डेटा सेटअप",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = BrandNavy
            )
            Text(
                text = if (step == 1) "100% Offline Billing & Ledger for your shop" else "Start fresh or restore from existing Google Sheet backup",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (step == 1) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Step 1: Shop Information / दुकान विवरण",
                            fontWeight = FontWeight.Bold,
                            color = BrandNavy,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = shopName,
                            onValueChange = { shopName = it },
                            label = { Text("Shop Name (दुकान का नाम) *") },
                            placeholder = { Text("e.g. Gupta Kirana Store") },
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_shop_name"),
                            singleLine = true,
                            colors = dukanTextFieldColors(),
                            textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            label = { Text("Owner Name (मालिक का नाम)") },
                            placeholder = { Text("e.g. Ramesh Gupta") },
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_owner_name"),
                            singleLine = true,
                            colors = dukanTextFieldColors(),
                            textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Medium)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number (फ़ोन नंबर)") },
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_phone"),
                            singleLine = true,
                            colors = dukanTextFieldColors(),
                            textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Medium),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = gstin,
                            onValueChange = { gstin = it },
                            label = { Text("GSTIN (Optional / छोड़ सकते हैं)") },
                            placeholder = { Text("22AAAAA0000A1Z5") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = dukanTextFieldColors(),
                            textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Medium)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Shop Address (दुकान का पता)") },
                            placeholder = { Text("e.g. Main Market, Station Road") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = dukanTextFieldColors(),
                            textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Medium)
                        )
                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                if (shopName.isBlank()) {
                                    Toast.makeText(context, "Please enter shop name", Toast.LENGTH_SHORT).show()
                                } else {
                                    step = 2
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("onboarding_next_btn")
                        ) {
                            Text("Next / आगे बढ़ें", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                        }
                    }
                }
            } else {
                // Step 2: Start Fresh or Restore
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Step 2: Choose Setup Mode",
                            fontWeight = FontWeight.Bold,
                            color = BrandNavy,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Option A: Start Fresh
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.completeOnboarding(shopName, ownerName, phone, gstin, address)
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(BrandNavy),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AddBusiness, contentDescription = null, tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Start Fresh / नई दुकान शुरू करें",
                                        fontWeight = FontWeight.Bold,
                                        color = BrandNavy
                                    )
                                    Text(
                                        text = "Clean database ready for your products and customers",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // Option B: Restore from Google Sheet or CSV
                        Text(
                            text = "Or Restore from Backup / बैकअप से रीस्टोर करें",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = backupSheetInput,
                            onValueChange = { backupSheetInput = it },
                            label = { Text("Google Sheet Link / ID (Optional)") },
                            placeholder = { Text("Paste sheet link to connect backup") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = dukanTextFieldColors(),
                            textStyle = LocalTextStyle.current.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Medium)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                viewModel.completeOnboarding(shopName, ownerName, phone, gstin, address)
                                if (backupSheetInput.isNotBlank()) {
                                    scope.launch {
                                        viewModel.syncManager.connectBackupSheet(backupSheetInput)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("connect_sheet_onboarding_btn")
                        ) {
                            Text("Connect Sheet & Open Shop", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showRestoreCsvDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Import Data from Backup Text / CSV")
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(
                            onClick = { step = 1 },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("← Back to Shop Info")
                        }
                    }
                }
            }
        }
    }

    if (showRestoreCsvDialog) {
        RestoreBackupDialog(
            onDismiss = { showRestoreCsvDialog = false },
            onRestore = { rawText ->
                scope.launch {
                    viewModel.completeOnboarding(shopName, ownerName, phone, gstin, address)
                    val result = viewModel.syncManager.restoreFromBackupData(rawText)
                    showRestoreCsvDialog = false
                    if (result.isSuccess) {
                        Toast.makeText(context, "Data Restored Successfully!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

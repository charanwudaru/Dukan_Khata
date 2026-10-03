package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ShopViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ShopViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = false, dynamicColor = false) {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val lang by viewModel.language.collectAsState()
                val userMessage by viewModel.userMessage.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(userMessage) {
                    userMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearUserMessage()
                    }
                }

                // Handle Back Button
                BackHandler(enabled = currentScreen != AppScreen.HOME && currentScreen != AppScreen.ONBOARDING) {
                    when (currentScreen) {
                        AppScreen.BILLING, AppScreen.BILL_DETAIL -> viewModel.navigateTo(AppScreen.HOME)
                        AppScreen.KHATA_CUSTOMER_DETAIL -> viewModel.navigateTo(AppScreen.KHATA)
                        AppScreen.STOCK, AppScreen.KHATA, AppScreen.REPORTS, AppScreen.SETTINGS -> viewModel.navigateTo(AppScreen.HOME)
                        else -> {}
                    }
                }

                val showBottomNav = currentScreen in listOf(
                    AppScreen.HOME,
                    AppScreen.STOCK,
                    AppScreen.KHATA,
                    AppScreen.REPORTS,
                    AppScreen.SETTINGS
                )

                Scaffold(
                    bottomBar = {
                        if (showBottomNav) {
                            AppBottomNavigation(
                                currentScreen = currentScreen,
                                onNavigate = { screen -> viewModel.navigateTo(screen) },
                                lang = lang
                            )
                        }
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = if (showBottomNav) innerPadding.calculateBottomPadding() else androidx.compose.ui.unit.Dp(0f))
                    ) {
                        when (currentScreen) {
                            AppScreen.ONBOARDING -> OnboardingScreen(viewModel = viewModel)
                            AppScreen.HOME -> HomeScreen(viewModel = viewModel)
                            AppScreen.STOCK -> StockScreen(viewModel = viewModel)
                            AppScreen.BILLING -> BillingScreen(viewModel = viewModel)
                            AppScreen.BILL_DETAIL -> BillDetailScreen(viewModel = viewModel)
                            AppScreen.KHATA -> KhataScreen(viewModel = viewModel)
                            AppScreen.KHATA_CUSTOMER_DETAIL -> KhataDetailScreen(viewModel = viewModel)
                            AppScreen.REPORTS -> ReportsScreen(viewModel = viewModel)
                            AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

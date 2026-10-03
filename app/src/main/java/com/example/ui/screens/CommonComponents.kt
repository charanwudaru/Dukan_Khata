package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.strings.LocaleStrings
import com.example.ui.viewmodel.AppScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Custom App Color Palette
val BrandNavy = Color(0xFF1A237E)
val BrandBlue = Color(0xFF1565C0)
val BrandSurface = Color(0xFFF8FAFC)
val CardBorderColor = Color(0xFFE2E8F0)
val SuccessGreen = Color(0xFF1B5E20)
val SuccessGreenBg = Color(0xFFE8F5E9)
val WarningAmber = Color(0xFFE65100)
val WarningAmberBg = Color(0xFFFFF3E0)
val DangerRed = Color(0xFFB71C1C)
val DangerRedBg = Color(0xFFFFEBEE)

fun formatRupee(amount: Double): String {
    return if (amount % 1.0 == 0.0) {
        "₹${amount.toLong()}"
    } else {
        "₹%.2f".format(amount)
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun AppBottomNavigation(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    lang: String
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        modifier = Modifier.navigationBarsPadding().testTag("bottom_nav_bar")
    ) {
        val items = listOf(
            Triple(AppScreen.HOME, Icons.Default.Home, LocaleStrings.get("home", lang)),
            Triple(AppScreen.STOCK, Icons.Default.Inventory2, LocaleStrings.get("stock", lang)),
            Triple(AppScreen.KHATA, Icons.Default.AccountBalanceWallet, LocaleStrings.get("khata", lang)),
            Triple(AppScreen.REPORTS, Icons.Default.BarChart, LocaleStrings.get("reports", lang)),
            Triple(AppScreen.SETTINGS, Icons.Default.Settings, LocaleStrings.get("settings", lang))
        )

        items.forEach { (screen, icon, label) ->
            val isSelected = when (currentScreen) {
                AppScreen.HOME -> screen == AppScreen.HOME
                AppScreen.STOCK -> screen == AppScreen.STOCK
                AppScreen.KHATA, AppScreen.KHATA_CUSTOMER_DETAIL -> screen == AppScreen.KHATA
                AppScreen.REPORTS -> screen == AppScreen.REPORTS
                AppScreen.SETTINGS -> screen == AppScreen.SETTINGS
                AppScreen.BILLING, AppScreen.BILL_DETAIL -> screen == AppScreen.HOME
                AppScreen.ONBOARDING -> false
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(screen) },
                icon = { Icon(imageVector = icon, contentDescription = label) },
                label = { Text(text = label, maxLines = 1, fontSize = 12.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandNavy,
                    selectedTextColor = BrandNavy,
                    indicatorColor = Color(0xFFE8EAF6),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                ),
                modifier = Modifier.testTag("nav_${screen.name.lowercase()}")
            )
        }
    }
}

@Composable
fun StatusBadge(
    text: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

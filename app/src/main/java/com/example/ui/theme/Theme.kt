package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.graphics.Color

val DukanNavy = Color(0xFF1A237E)
val DukanBlue = Color(0xFF1565C0)
val DukanAmber = Color(0xFFE65100)
val DukanGreen = Color(0xFF1B5E20)
val DukanSurface = Color(0xFFF8FAFC)
val DukanTextPrimary = Color(0xFF0F172A)
val DukanTextSecondary = Color(0xFF475569)

@Composable
fun dukanTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = DukanTextPrimary,
    unfocusedTextColor = DukanTextPrimary,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color(0xFFF8FAFC),
    focusedBorderColor = DukanNavy,
    unfocusedBorderColor = Color(0xFF94A3B8),
    focusedLabelColor = DukanNavy,
    unfocusedLabelColor = Color(0xFF475569),
    cursorColor = DukanNavy,
    focusedPlaceholderColor = Color(0xFF94A3B8),
    unfocusedPlaceholderColor = Color(0xFF94A3B8),
    focusedLeadingIconColor = DukanNavy,
    unfocusedLeadingIconColor = Color(0xFF64748B),
    focusedTrailingIconColor = DukanNavy,
    unfocusedTrailingIconColor = Color(0xFF64748B),
    focusedPrefixColor = DukanTextPrimary,
    unfocusedPrefixColor = DukanTextPrimary,
    focusedSuffixColor = Color(0xFF475569),
    unfocusedSuffixColor = Color(0xFF475569)
)

private val DarkColorScheme =
  lightColorScheme(
    primary = DukanNavy,
    secondary = DukanAmber,
    tertiary = DukanGreen,
    background = DukanSurface,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = DukanTextPrimary,
    onSurface = DukanTextPrimary,
    onSurfaceVariant = DukanTextSecondary
  )

private val LightColorScheme =
  lightColorScheme(
    primary = DukanNavy,
    secondary = DukanAmber,
    tertiary = DukanGreen,
    background = DukanSurface,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = DukanTextPrimary,
    onSurface = DukanTextPrimary,
    onSurfaceVariant = DukanTextSecondary
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

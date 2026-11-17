package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SeniorIvoryColorScheme =
  lightColorScheme(
    primary = LeafGreen,
    onPrimary = Color.White,
    primaryContainer = LeafGreenSoft,
    onPrimaryContainer = LeafGreenDark,
    secondary = OceanBlue,
    onSecondary = Color.White,
    secondaryContainer = OceanBlueSurface,
    onSecondaryContainer = OceanBlueDark,
    tertiary = WarmAmber,
    onTertiary = Color.White,
    background = IvoryBackground,
    onBackground = DeepCharcoal,
    surface = IvorySurface,
    onSurface = DeepCharcoal,
    surfaceVariant = IvorySurfaceWarm,
    onSurfaceVariant = WarmSlate,
    outline = IvoryBorder,
    error = ErrorCrimson,
    onError = Color.White,
    errorContainer = ErrorSoft,
    onErrorContainer = ErrorCrimson,
  )

@Composable
fun SeniorCareTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = SeniorIvoryColorScheme,
    typography = SeniorTypography,
    content = content,
  )
}

package com.swiftshare.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryAccent,
    onPrimary = NeutralWhite,
    primaryContainer = PrimaryAccent.copy(alpha = 0.2f),
    onPrimaryContainer = PrimaryCyan,
    secondary = PrimaryCyan,
    onSecondary = NeutralBlack,
    secondaryContainer = PrimaryCyan.copy(alpha = 0.15f),
    onSecondaryContainer = PrimaryCyan,
    tertiary = SecondaryPurple,
    onTertiary = NeutralWhite,
    error = SecondaryPink,
    onError = NeutralWhite,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = PrimarySurface.copy(alpha = 0.7f),
    onSurfaceVariant = NeutralGray400,
    outline = NeutralGray600,
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryAccent,
    onPrimary = NeutralWhite,
    primaryContainer = PrimaryAccent.copy(alpha = 0.1f),
    onPrimaryContainer = PrimaryAccent,
    secondary = PrimaryCyan,
    onSecondary = NeutralBlack,
    secondaryContainer = PrimaryCyan.copy(alpha = 0.1f),
    onSecondaryContainer = Color(0xFF006874),
    tertiary = SecondaryPurple,
    onTertiary = NeutralWhite,
    error = SecondaryPink,
    onError = NeutralWhite,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = NeutralGray100,
    onSurfaceVariant = NeutralGray600,
    outline = NeutralGray200,
)

@Composable
fun SwiftShareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SwiftShareTypography,
        content = content,
    )
}

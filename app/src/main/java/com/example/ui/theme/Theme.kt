package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = FlowRed,
    onPrimary = Color.White,
    primaryContainer = FlowRedVariant,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFE50914),
    onSecondary = Color.White,
    background = FlowDarkBackground,
    onBackground = FlowDarkTextPrimary,
    surface = FlowDarkBackground,
    onSurface = FlowDarkTextPrimary,
    surfaceVariant = FlowDarkSurfaceVariant,
    onSurfaceVariant = FlowDarkTextSecondary,
    outline = FlowDarkBorder,
    outlineVariant = FlowDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = FlowRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEBEE),
    onPrimaryContainer = FlowRedDark,
    secondary = Color(0xFFD32F2F),
    onSecondary = Color.White,
    background = FlowLightBackground,
    onBackground = FlowLightTextPrimary,
    surface = FlowLightBackground,
    onSurface = FlowLightTextPrimary,
    surfaceVariant = FlowLightSurfaceVariant,
    onSurfaceVariant = FlowLightTextSecondary,
    outline = FlowLightBorder,
    outlineVariant = FlowLightBorder
)

@Composable
fun FlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve brand YouTube-like red & dark/light theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

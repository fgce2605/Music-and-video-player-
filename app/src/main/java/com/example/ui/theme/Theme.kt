package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = OmniDarkPrimary,
    onPrimary = OmniDarkOnPrimary,
    secondary = OmniDarkSecondary,
    tertiary = OmniDarkTertiary,
    background = OmniDarkBackground,
    surface = OmniDarkSurface,
    surfaceVariant = OmniDarkSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val LightColorScheme = lightColorScheme(
    primary = OmniLightPrimary,
    onPrimary = OmniLightOnPrimary,
    secondary = OmniLightSecondary,
    tertiary = OmniLightTertiary,
    background = OmniLightBackground,
    surface = OmniLightSurface,
    surfaceVariant = OmniLightSurfaceVariant,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun OmniPlayTheme(
    darkTheme: Boolean = true, // Dark theme by default per spec
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    OmniPlayTheme(darkTheme = darkTheme, content = content)
}


package com.example.roadbookorganizador.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = FredianiCyan,
    secondary = FredianiAmber,
    tertiary = FredianiGreen,
    background = RallyDarkBg,
    surface = RallyCardBg,
    surfaceVariant = RallySurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = FredianiCyanText,
    secondary = FredianiAmberText,
    tertiary = FredianiGreenText,
    background = Color(0xFFF1F5F9),
    surface = Color.White,
    surfaceVariant = Color(0xFFE2E8F0),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = FredianiTextDark,
    onSurface = FredianiTextDark,
    onSurfaceVariant = FredianiTextMuted
)

@Composable
fun RoadbookOrganizadorTheme(
    darkTheme: Boolean = ThemeManager.isDarkTheme,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

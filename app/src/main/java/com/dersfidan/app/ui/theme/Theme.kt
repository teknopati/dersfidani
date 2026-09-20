package com.dersfidan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = YesilAna,
    onPrimary = Krem,
    primaryContainer = Color(0xFFCDE8C5),
    onPrimaryContainer = Color(0xFF102D16),
    secondary = YesilKoyu,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE9D8),
    onSecondaryContainer = Color(0xFF172817),
    tertiary = Color(0xFF8A5D2E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDBB),
    onTertiaryContainer = Color(0xFF2D1600),
    background = YesilArkaplan,
    onBackground = Color(0xFF182018),
    surface = Krem,
    onSurface = Color(0xFF1B201A),
    surfaceVariant = Color(0xFFE2E9DE),
    onSurfaceVariant = Color(0xFF414940),
    outline = Color(0xFF717970),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF91D58A),
    onPrimary = Color(0xFF17331E),
    primaryContainer = Color(0xFF355A3D),
    onPrimaryContainer = Color(0xFFE3F4D9),
    secondary = Color(0xFFB7CFAE),
    onSecondary = Color(0xFF223121),
    secondaryContainer = Color(0xFF3A513C),
    onSecondaryContainer = Color(0xFFE1EDDA),
    tertiary = Color(0xFFE4B77D),
    onTertiary = Color(0xFF412C0B),
    tertiaryContainer = Color(0xFF5B421F),
    onTertiaryContainer = Color(0xFFFFDDB0),
    background = Color(0xFF142119),
    onBackground = Color(0xFFF1E7D5),
    surface = Color(0xFF25372C),
    onSurface = Color(0xFFF4E9D6),
    surfaceVariant = Color(0xFF354C3D),
    onSurfaceVariant = Color(0xFFD6CBB9),
    outline = Color(0xFF9BAE9C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun DersFidanTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}

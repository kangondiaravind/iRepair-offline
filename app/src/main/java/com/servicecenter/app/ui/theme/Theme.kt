package com.servicecenter.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

private val LightColors = lightColorScheme(
    primary = Color(0xFF087F8C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC9F0F0),
    onPrimaryContainer = Color(0xFF003A40),
    secondary = Color(0xFF526879),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5E5F0),
    onSecondaryContainer = Color(0xFF102D3D),
    background = Color(0xFFF4F7F9),
    onBackground = Color(0xFF172A35),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF172A35),
    surfaceVariant = Color(0xFFEAF0F3),
    onSurfaceVariant = Color(0xFF52636D),
    outline = Color(0xFF8799A3),
    outlineVariant = Color(0xFFD7E0E5),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7BD5D8),
    onPrimary = Color(0xFF00363B),
    primaryContainer = Color(0xFF005057),
    onPrimaryContainer = Color(0xFFC9F0F0),
    secondary = Color(0xFFB9CAD6),
    onSecondary = Color(0xFF243746),
    secondaryContainer = Color(0xFF3A5060),
    onSecondaryContainer = Color(0xFFD5E5F0),
    background = Color(0xFF101D24),
    onBackground = Color(0xFFE0E9ED),
    surface = Color(0xFF17262E),
    onSurface = Color(0xFFE0E9ED),
    surfaceVariant = Color(0xFF263740),
    onSurfaceVariant = Color(0xFFB7C7CE),
    outline = Color(0xFF82949D),
    outlineVariant = Color(0xFF3E5059),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val AppTypography = Typography().let { defaults ->
    defaults.copy(
        headlineSmall = defaults.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
        titleLarge = defaults.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
        titleMedium = defaults.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = defaults.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun ServiceCenterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

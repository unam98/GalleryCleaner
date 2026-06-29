package com.unam.gallerycleaner.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = iOSBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEBF3FF),
    onPrimaryContainer = Color(0xFF004AAE),
    secondary = iOSSecondaryLabel,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF2F2F7),
    onSecondaryContainer = Color(0xFF3A3A3C),
    tertiary = iOSOrange,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF3D9),
    onTertiaryContainer = Color(0xFF7A4800),
    background = iOSGroupedBg,
    onBackground = iOSLabel,
    surface = iOSSecondaryGroupedBg,
    onSurface = iOSLabel,
    surfaceVariant = iOSTertiaryGroupedBg,
    onSurfaceVariant = iOSSecondaryLabel,
    error = iOSRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFE5E3),
    onErrorContainer = Color(0xFFB81600),
    outline = iOSSeparator,
    outlineVariant = iOSSeparatorOpaque,
    scrim = Color(0xFF000000),
)

private val DarkColorScheme = darkColorScheme(
    primary = iOSBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF003D8A),
    onPrimaryContainer = Color(0xFF99C9FF),
    secondary = iOSSecondaryLabelDark,
    onSecondary = Color.Black,
    secondaryContainer = iOSTertiaryGroupedBgDark,
    onSecondaryContainer = Color(0xFFEBEBF5),
    tertiary = iOSOrangeDark,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF3A2D00),
    onTertiaryContainer = Color(0xFFFFE08A),
    background = iOSGroupedBgDark,
    onBackground = iOSLabelDark,
    surface = iOSSecondaryGroupedBgDark,
    onSurface = iOSLabelDark,
    surfaceVariant = iOSTertiaryGroupedBgDark,
    onSurfaceVariant = iOSSecondaryLabelDark,
    error = iOSRedDark,
    onError = Color.White,
    errorContainer = Color(0xFF4A0A08),
    onErrorContainer = Color(0xFFFFB4AB),
    outline = iOSSeparatorDark,
    outlineVariant = iOSSeparatorOpaqueDark,
    scrim = Color(0xFF000000),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun GalleryCleanerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}

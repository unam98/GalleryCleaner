package com.namilab.gallerycleaner.presentation.ui.theme

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
    primary = BrandFresh,
    // 흰 글씨(대비 2.35:1)보다 잉크(다크) 텍스트(7.56:1)가 대비가 훨씬 좋다.
    onPrimary = iOSLabel,
    primaryContainer = Color(0xFFD6F1FA),
    onPrimaryContainer = Color(0xFF0A4E63),
    secondary = iOSSecondaryLabel,
    onSecondary = Color.White,
    secondaryContainer = iOSTertiaryGroupedBg,
    onSecondaryContainer = Color(0xFF3A3025),
    tertiary = iOSOrange,
    onTertiary = iOSLabel,
    tertiaryContainer = Color(0xFFFFF0D2),
    onTertiaryContainer = Color(0xFF7A5300),
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
    primary = BrandFreshDark,
    // 흰 글씨(대비 1.99:1)보다 잉크 텍스트(8.93:1)가 대비가 훨씬 좋다.
    onPrimary = iOSLabel,
    primaryContainer = Color(0xFF0E4258),
    onPrimaryContainer = Color(0xFFB8E6F5),
    secondary = iOSSecondaryLabelDark,
    onSecondary = Color.Black,
    secondaryContainer = iOSTertiaryGroupedBgDark,
    onSecondaryContainer = Color(0xFFEBEBF5),
    tertiary = iOSOrangeDark,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF4A3200),
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

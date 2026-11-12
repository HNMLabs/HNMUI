package com.hnmlabs.hnmui.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object HnmPalette {
    val GlassSurfacePrimary = Color(0x52182232)
    val GlassSurfaceElevated = Color(0x78141C2B)
    val GlassDockTop = Color(0x66FFFFFF)
    val GlassDockFillTop = Color(0x42D8E4F8)
    val GlassDockFillBottom = Color(0x3094A3B8)
    val GlassCardFillTop = Color(0x581E293B)
    val GlassCardFillBottom = Color(0x440F172A)
    val SpecularHighlight = Color(0x59FFFFFF)
    val SubtleBorder = Color(0x26FFFFFF)

    val AccentCyan = Color(0xFF38BDF8)
    val AccentBlue = Color(0xFF0A84FF)
    val BadgeRed = Color(0xFFFF3B30)
    val SuccessGreen = Color(0xFF30D158)

    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xCCDBEAFE)
    val TextMuted = Color(0x99CBD5E1)
    val ScrimOverlay = Color(0x94090E17)
}

/**
 * Açık renkli duvar kağıtlarında bile uygulama etiketlerinin net okunabilmesi için
 * hafif optik gölge içeren tipografi stili.
 */
val WallpaperLabelShadow = Shadow(
    color = Color(0x99000000),
    offset = Offset(0f, 1.5f),
    blurRadius = 4f
)

private val HnmColorScheme = darkColorScheme(
    primary = HnmPalette.AccentBlue,
    secondary = HnmPalette.AccentCyan,
    background = Color.Transparent,
    surface = HnmPalette.GlassSurfacePrimary,
    onPrimary = Color.White,
    onBackground = HnmPalette.TextPrimary,
    onSurface = HnmPalette.TextPrimary,
    error = HnmPalette.BadgeRed
)

private val HnmTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.6).sp,
        color = HnmPalette.TextPrimary
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp,
        color = HnmPalette.TextPrimary
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = HnmPalette.TextPrimary
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = HnmPalette.TextSecondary
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        color = HnmPalette.TextPrimary,
        shadow = WallpaperLabelShadow
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White
    )
)

@Composable
fun HnmLauncherTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HnmColorScheme,
        typography = HnmTypography,
        content = content
    )
}

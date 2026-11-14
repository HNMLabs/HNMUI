package com.hnmlabs.hnmui.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hnmlabs.hnmui.model.HnmThemeMode

private val AppearanceTransitionEasing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)
private const val APPEARANCE_TRANSITION_MS = 380

/**
 * Sistem karanlık mod bayrağı ve kullanıcı tercihine göre etkin karanlık mod durumunu çözümler.
 */
fun resolveIsDarkTheme(mode: HnmThemeMode, systemDark: Boolean): Boolean = when (mode) {
    HnmThemeMode.SYSTEM -> systemDark
    HnmThemeMode.DARK -> true
    HnmThemeMode.LIGHT -> false
}

/**
 * HNMUI Koyu (Midnight Glass) ve Açık (Daylight Pearlescent Glass) modlarının tamamını
 * tek çatı altında toplayan anlamsal renk jetonları.
 */
@Immutable
data class HnmColors(
    val isDark: Boolean,
    val glassDockFillTop: Color,
    val glassDockFillBottom: Color,
    val glassCardFillTop: Color,
    val glassCardFillBottom: Color,
    val glassModalFillTop: Color,
    val glassModalFillBottom: Color,
    val glassPillFillTop: Color,
    val glassPillFillBottom: Color,
    val specularHighlight: Color,
    val subtleBorder: Color,
    val glassRimBottom: Color,
    val scrimOverlay: Color,
    val controlCenterBackdrop: Color,
    val coverSheetTop: Color,
    val coverSheetMiddle: Color,
    val coverSheetBottom: Color,
    val settingsBackdropTop: Color,
    val settingsBackdropBottom: Color,
    val wallpaperScrimTop: Color,
    val wallpaperScrimMid: Color,
    val wallpaperScrimBottom: Color,
    val surfaceSubtleFill: Color,
    val controlInactiveFill: Color,
    val sliderTrackInactive: Color,
    val verticalSliderFill: Color,
    val verticalSliderIconActive: Color,
    val verticalSliderIconIdle: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val onWallpaperPrimary: Color,
    val onWallpaperSecondary: Color,
    val accentBlue: Color,
    val accentCyan: Color,
    val successGreen: Color,
    val badgeRed: Color
)

val DarkHnmColors = HnmColors(
    isDark = true,
    glassDockFillTop = Color(0x42D8E4F8),
    glassDockFillBottom = Color(0x3094A3B8),
    glassCardFillTop = Color(0x70162032),
    glassCardFillBottom = Color(0x5C0F172A),
    glassModalFillTop = Color(0xE01E293B),
    glassModalFillBottom = Color(0xD90F172A),
    glassPillFillTop = Color(0x42FFFFFF),
    glassPillFillBottom = Color(0x28CBD5E1),
    specularHighlight = Color(0x59FFFFFF),
    subtleBorder = Color(0x26FFFFFF),
    glassRimBottom = Color(0x16FFFFFF),
    scrimOverlay = Color(0x9E080D18),
    controlCenterBackdrop = Color(0xBD080D18),
    coverSheetTop = Color(0xD1070C17),
    coverSheetMiddle = Color(0xDF090F1D),
    coverSheetBottom = Color(0xED070B14),
    settingsBackdropTop = Color(0xF00B1220),
    settingsBackdropBottom = Color(0xF8070C16),
    wallpaperScrimTop = Color(0x3D070B14),
    wallpaperScrimMid = Color(0x14070B14),
    wallpaperScrimBottom = Color(0x4D070B14),
    surfaceSubtleFill = Color(0x1FFFFFFF),
    controlInactiveFill = Color(0x28FFFFFF),
    sliderTrackInactive = Color(0x30FFFFFF),
    verticalSliderFill = Color(0xDBFFFFFF),
    verticalSliderIconActive = Color(0xFF0F172A),
    verticalSliderIconIdle = Color(0xFFF8FAFC),
    divider = Color(0x21FFFFFF),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xD9DBEAFE),
    textMuted = Color(0x9ECBD5E1),
    onWallpaperPrimary = Color(0xFFF8FAFC),
    onWallpaperSecondary = Color(0xE0E2E8F0),
    accentBlue = Color(0xFF0A84FF),
    accentCyan = Color(0xFF38BDF8),
    successGreen = Color(0xFF30D158),
    badgeRed = Color(0xFFFF3B30)
)

val LightHnmColors = HnmColors(
    isDark = false,
    glassDockFillTop = Color(0xD6FFFFFF),
    glassDockFillBottom = Color(0xC2E2E8F0),
    glassCardFillTop = Color(0xE3FFFFFF),
    glassCardFillBottom = Color(0xD6EDF2F9),
    glassModalFillTop = Color(0xF5FFFFFF),
    glassModalFillBottom = Color(0xEDF1F5FB),
    glassPillFillTop = Color(0xE6FFFFFF),
    glassPillFillBottom = Color(0xD9E8EEF7),
    specularHighlight = Color(0xEBFFFFFF),
    subtleBorder = Color(0x240F172A),
    glassRimBottom = Color(0x160F172A),
    scrimOverlay = Color(0xD9E5ECF6),
    controlCenterBackdrop = Color(0xE3E6EEF8),
    coverSheetTop = Color(0xEDF1F6FC),
    coverSheetMiddle = Color(0xF3EEF3FA),
    coverSheetBottom = Color(0xF8E6EDF7),
    settingsBackdropTop = Color(0xF7F3F6FB),
    settingsBackdropBottom = Color(0xFCE8EEF7),
    wallpaperScrimTop = Color(0x280B1220),
    wallpaperScrimMid = Color(0x0DFFFFFF),
    wallpaperScrimBottom = Color(0x2E0B1220),
    surfaceSubtleFill = Color(0x120F172A),
    controlInactiveFill = Color(0x170F172A),
    sliderTrackInactive = Color(0x240F172A),
    verticalSliderFill = Color(0xEB007AFF),
    verticalSliderIconActive = Color(0xFFFFFFFF),
    verticalSliderIconIdle = Color(0xFF1E293B),
    divider = Color(0x1A0F172A),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF334155),
    textMuted = Color(0xFF64748B),
    onWallpaperPrimary = Color(0xFFF8FAFC),
    onWallpaperSecondary = Color(0xEBEEF2F9),
    accentBlue = Color(0xFF007AFF),
    accentCyan = Color(0xFF0284C7),
    successGreen = Color(0xFF16A34A),
    badgeRed = Color(0xFFFF3B30)
)

val LocalHnmColors = compositionLocalOf { DarkHnmColors }

object HnmTheme {
    val colors: HnmColors
        @Composable
        @ReadOnlyComposable
        get() = LocalHnmColors.current
}

/**
 * Geriye dönük ve canlı tema uyumlu anlamsal renk erişim köprüsü.
 * Tüm özellikleri LocalHnmColors üzerinden okur; böylece Açık <-> Koyu geçişinde otomatik canlanır.
 */
object HnmPalette {
    val GlassSurfacePrimary: Color
        @Composable @ReadOnlyComposable get() = LocalHnmColors.current.glassCardFillTop
    val GlassSurfaceElevated: Color
        @Composable @ReadOnlyComposable get() = LocalHnmColors.current.glassModalFillTop
    val GlassDockFillTop: Color
        @Composable @ReadOnlyComposable get() = LocalHnmColors.current.glassDockFillTop
    val GlassDockFillBottom: Color
        @Composable @ReadOnlyComposable get() = LocalHnmColors.current.glassDockFillBottom
    val SpecularHighlight: Color
        @Composable @ReadOnlyComposable get() = LocalHnmColors.current.specularHighlight
    val SubtleBorder: Color
        @Composable @ReadOnlyComposable get() = LocalHnmColors.current.subtleBorder

    val AccentCyan: Color
        @Composable @ReadOnlyComposable get() = LocalHnmColors.current.accentCyan
    val AccentBlue: Color
        @Composable @ReadOnlyComposable get() = LocalHnmColors.current.accentBlue
    val BadgeRed: Color = Color(0xFFFF3B30)
    val SuccessGreen: Color
        @Composable @ReadOnlyComposable get() = LocalHnmColors.current.successGreen

    val TextPrimary: Color
        @Composable @ReadOnlyComposable get() = LocalHnmColors.current.textPrimary
    val TextSecondary: Color
        @Composable @ReadOnlyComposable get() = LocalHnmColors.current.textSecondary
    val TextMuted: Color
        @Composable @ReadOnlyComposable get() = LocalHnmColors.current.textMuted
    val ScrimOverlay: Color
        @Composable @ReadOnlyComposable get() = LocalHnmColors.current.scrimOverlay
}

/**
 * Açık renkli duvar kağıtlarında bile ana ekran uygulama etiketlerinin net okunabilmesi için
 * hafif optik gölge içeren tipografi stili.
 */
val WallpaperLabelShadow = Shadow(
    color = Color(0x99000000),
    offset = Offset(0f, 1.5f),
    blurRadius = 4f
)

@Composable
private fun animateThemeToken(target: Color, label: String): Color {
    val animated by animateColorAsState(
        targetValue = target,
        animationSpec = tween(
            durationMillis = APPEARANCE_TRANSITION_MS,
            easing = AppearanceTransitionEasing
        ),
        label = label
    )
    return animated
}

@Composable
fun HnmLauncherTheme(
    themeMode: HnmThemeMode = HnmThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = resolveIsDarkTheme(mode = themeMode, systemDark = isSystemInDarkTheme())
    val target = if (isDark) DarkHnmColors else LightHnmColors

    val animatedColors = HnmColors(
        isDark = isDark,
        glassDockFillTop = animateThemeToken(target.glassDockFillTop, "glassDockTop"),
        glassDockFillBottom = animateThemeToken(target.glassDockFillBottom, "glassDockBottom"),
        glassCardFillTop = animateThemeToken(target.glassCardFillTop, "glassCardTop"),
        glassCardFillBottom = animateThemeToken(target.glassCardFillBottom, "glassCardBottom"),
        glassModalFillTop = animateThemeToken(target.glassModalFillTop, "glassModalTop"),
        glassModalFillBottom = animateThemeToken(target.glassModalFillBottom, "glassModalBottom"),
        glassPillFillTop = animateThemeToken(target.glassPillFillTop, "glassPillTop"),
        glassPillFillBottom = animateThemeToken(target.glassPillFillBottom, "glassPillBottom"),
        specularHighlight = animateThemeToken(target.specularHighlight, "specularHighlight"),
        subtleBorder = animateThemeToken(target.subtleBorder, "subtleBorder"),
        glassRimBottom = animateThemeToken(target.glassRimBottom, "glassRimBottom"),
        scrimOverlay = animateThemeToken(target.scrimOverlay, "scrimOverlay"),
        controlCenterBackdrop = animateThemeToken(target.controlCenterBackdrop, "ccBackdrop"),
        coverSheetTop = animateThemeToken(target.coverSheetTop, "coverSheetTop"),
        coverSheetMiddle = animateThemeToken(target.coverSheetMiddle, "coverSheetMiddle"),
        coverSheetBottom = animateThemeToken(target.coverSheetBottom, "coverSheetBottom"),
        settingsBackdropTop = animateThemeToken(target.settingsBackdropTop, "settingsTop"),
        settingsBackdropBottom = animateThemeToken(target.settingsBackdropBottom, "settingsBottom"),
        wallpaperScrimTop = animateThemeToken(target.wallpaperScrimTop, "wallScrimTop"),
        wallpaperScrimMid = animateThemeToken(target.wallpaperScrimMid, "wallScrimMid"),
        wallpaperScrimBottom = animateThemeToken(target.wallpaperScrimBottom, "wallScrimBottom"),
        surfaceSubtleFill = animateThemeToken(target.surfaceSubtleFill, "surfaceSubtleFill"),
        controlInactiveFill = animateThemeToken(target.controlInactiveFill, "controlInactive"),
        sliderTrackInactive = animateThemeToken(target.sliderTrackInactive, "sliderTrackInactive"),
        verticalSliderFill = animateThemeToken(target.verticalSliderFill, "verticalSliderFill"),
        verticalSliderIconActive = animateThemeToken(target.verticalSliderIconActive, "vSliderIconActive"),
        verticalSliderIconIdle = animateThemeToken(target.verticalSliderIconIdle, "vSliderIconIdle"),
        divider = animateThemeToken(target.divider, "divider"),
        textPrimary = animateThemeToken(target.textPrimary, "textPrimary"),
        textSecondary = animateThemeToken(target.textSecondary, "textSecondary"),
        textMuted = animateThemeToken(target.textMuted, "textMuted"),
        onWallpaperPrimary = target.onWallpaperPrimary,
        onWallpaperSecondary = target.onWallpaperSecondary,
        accentBlue = animateThemeToken(target.accentBlue, "accentBlue"),
        accentCyan = animateThemeToken(target.accentCyan, "accentCyan"),
        successGreen = animateThemeToken(target.successGreen, "successGreen"),
        badgeRed = target.badgeRed
    )

    // KRİTİK 120 FPS: MaterialTheme'in iç LocalColorScheme ve LocalTypography sağlayıcıları
    // staticCompositionLocalOf kullandığı için YALNIZCA isDark değiştiğinde (1 kez) güncellenir;
    // ara karelerdeki yumuşak metin ve yüzey geçişleri izlenebilir LocalHnmColors + LocalContentColor ile akar!
    val colorScheme = remember(isDark) {
        if (isDark) {
            darkColorScheme(
                primary = target.accentBlue,
                secondary = target.accentCyan,
                background = Color.Transparent,
                surface = target.glassCardFillTop,
                onPrimary = Color.White,
                onBackground = target.textPrimary,
                onSurface = target.textPrimary,
                error = target.badgeRed
            )
        } else {
            lightColorScheme(
                primary = target.accentBlue,
                secondary = target.accentCyan,
                background = Color.Transparent,
                surface = target.glassCardFillTop,
                onPrimary = Color.White,
                onBackground = target.textPrimary,
                onSurface = target.textPrimary,
                error = target.badgeRed
            )
        }
    }

    val typography = remember(isDark) {
        Typography(
            headlineLarge = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp,
                color = Color.Unspecified
            ),
            titleLarge = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
                color = Color.Unspecified
            ),
            titleMedium = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Unspecified
            ),
            titleSmall = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Unspecified
            ),
            bodyMedium = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = target.textSecondary
            ),
            bodySmall = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = target.textSecondary
            ),
            labelMedium = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp,
                color = Color.Unspecified
            ),
            labelSmall = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Unspecified
            )
        )
    }

    CompositionLocalProvider(LocalHnmColors provides animatedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography
        ) {
            CompositionLocalProvider(
                LocalContentColor provides animatedColors.textPrimary,
                content = content
            )
        }
    }
}


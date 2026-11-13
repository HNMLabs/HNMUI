package com.hnmlabs.hnmui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hnmlabs.hnmui.ui.theme.HnmTheme
import kotlin.math.roundToInt

val LocalGlassOpacityFactor = compositionLocalOf { 1f }

enum class GlassTone {
    DOCK,
    CARD,
    ELEVATED_MODAL,
    PILL
}

/**
 * HNMUI Buzlu Cam (Frosted Glass) yüzeyi.
 * Hem Koyu (Midnight Glass) hem Açık (Daylight Pearlescent Glass) görünüm modlarına
 * ve kullanıcının seçtiği cam yoğunluğu çarpanına (LocalGlassOpacityFactor) canlı uyum sağlar.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    tone: GlassTone = GlassTone.CARD,
    shadowElevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = HnmTheme.colors
    val quantizedOpacity = ((LocalGlassOpacityFactor.current.coerceIn(0.65f, 1.40f) * 100f)
        .roundToInt()) / 100f

    val (rawTop, rawBottom) = when (tone) {
        GlassTone.DOCK -> colors.glassDockFillTop to colors.glassDockFillBottom
        GlassTone.CARD -> colors.glassCardFillTop to colors.glassCardFillBottom
        GlassTone.ELEVATED_MODAL -> colors.glassModalFillTop to colors.glassModalFillBottom
        GlassTone.PILL -> colors.glassPillFillTop to colors.glassPillFillBottom
    }

    val fillBrush = remember(rawTop, rawBottom, quantizedOpacity, colors.isDark) {
        val minAlpha = if (colors.isDark) 0.12f else 0.56f
        fun scaleAlpha(color: Color): Color =
            color.copy(alpha = (color.alpha * quantizedOpacity).coerceIn(minAlpha, 0.98f))

        Brush.verticalGradient(
            colors = listOf(scaleAlpha(rawTop), scaleAlpha(rawBottom))
        )
    }

    val borderBrush = remember(
        colors.specularHighlight,
        colors.subtleBorder,
        colors.glassRimBottom
    ) {
        Brush.verticalGradient(
            colors = listOf(
                colors.specularHighlight,
                colors.subtleBorder,
                colors.glassRimBottom
            )
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush = fillBrush, shape = shape)
            .border(width = 1.dp, brush = borderBrush, shape = shape),
        content = content
    )
}

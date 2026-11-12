package com.hnmlabs.hnmui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hnmlabs.hnmui.ui.theme.HnmPalette

enum class GlassTone {
    DOCK,
    CARD,
    ELEVATED_MODAL,
    PILL
}

/**
 * HNMUI Buzlu Cam (Frosted Glass) yüzeyi.
 * Not: Yarı saydam yüzeylerde Android HWUI Modifier.shadow iç bölgede koyu bir dikdörtgen
 * (inner umbra) izi bıraktığı ve 120 FPS GPU akıcılığını düşürdüğü için donanım gölgesi
 * kullanılmaz; derinlik tamamen pürüzsüz cam gradyanı ve optik kenar ışıltısıyla sağlanır.
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
    val fillBrush = when (tone) {
        GlassTone.DOCK -> Brush.verticalGradient(
            colors = listOf(HnmPalette.GlassDockFillTop, HnmPalette.GlassDockFillBottom)
        )
        GlassTone.CARD -> Brush.verticalGradient(
            colors = listOf(Color(0x70162032), Color(0x5C0F172A))
        )
        GlassTone.ELEVATED_MODAL -> Brush.verticalGradient(
            colors = listOf(Color(0xE01E293B), Color(0xD90F172A))
        )
        GlassTone.PILL -> Brush.verticalGradient(
            colors = listOf(Color(0x42FFFFFF), Color(0x28CBD5E1))
        )
    }

    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            HnmPalette.SpecularHighlight,
            HnmPalette.SubtleBorder,
            Color(0x16FFFFFF)
        )
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush = fillBrush, shape = shape)
            .border(width = 1.dp, brush = borderBrush, shape = shape),
        content = content
    )
}

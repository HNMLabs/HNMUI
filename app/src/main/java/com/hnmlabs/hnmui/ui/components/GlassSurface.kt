package com.hnmlabs.hnmui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
 * Üst kenarda ince optik ışık kırılması (specular highlight) ve yarı saydam katmanlı
 * gradyan ile hem açık hem koyu duvar kağıtları üzerinde derinlik hissi oluşturur.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    tone: GlassTone = GlassTone.CARD,
    shadowElevation: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val fillBrush = when (tone) {
        GlassTone.DOCK -> Brush.verticalGradient(
            colors = listOf(HnmPalette.GlassDockFillTop, HnmPalette.GlassDockFillBottom)
        )
        GlassTone.CARD -> Brush.verticalGradient(
            colors = listOf(HnmPalette.GlassCardFillTop, HnmPalette.GlassCardFillBottom)
        )
        GlassTone.ELEVATED_MODAL -> Brush.verticalGradient(
            colors = listOf(Color(0xCC1E293B), Color(0xBF0F172A))
        )
        GlassTone.PILL -> Brush.verticalGradient(
            colors = listOf(Color(0x47FFFFFF), Color(0x29CBD5E1))
        )
    }

    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            HnmPalette.SpecularHighlight,
            HnmPalette.SubtleBorder,
            Color(0x14FFFFFF)
        )
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                ambientColor = Color(0x55000000),
                spotColor = Color(0x66000000)
            )
            .clip(shape)
            .background(brush = fillBrush, shape = shape)
            .border(width = 1.dp, brush = borderBrush, shape = shape),
        content = content
    )
}

package com.hnmlabs.hnmui.ui.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.withSign

/**
 * Sürekli eğrilikli süper-elips (Squircle: |x/a|^n + |y/b|^n = 1).
 * Standart RoundedCornerShape'deki sert yay birleşimlerini ortadan kaldırarak
 * iOS ikonlarının dingin ve pürüzsüz siluetini üretir.
 */
class SquircleShape(
    private val exponent: Float = 4.4f,
    private val steps: Int = 72
) : Shape {

    internal val unitX = FloatArray(steps.coerceAtLeast(16))
    internal val unitY = FloatArray(steps.coerceAtLeast(16))

    internal val outlineCache = object : LinkedHashMap<Long, Outline>(48, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Outline>?): Boolean {
            return size > MAX_CACHED_OUTLINES
        }
    }

    init {
        val safeSteps = unitX.size
        val power = 2f / exponent
        for (i in 0 until safeSteps) {
            val theta = (2.0 * PI * i) / safeSteps
            val cosT = cos(theta).toFloat()
            val sinT = sin(theta).toFloat()
            unitX[i] = abs(cosT).pow(power).withSign(cosT)
            unitY[i] = abs(sinT).pow(power).withSign(sinT)
        }
    }

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline = resolveOutline(size) { halfW, halfH ->
        val path = Path()
        for (i in unitX.indices) {
            val x = halfW + halfW * unitX[i]
            val y = halfH + halfH * unitY[i]
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        Outline.Generic(path)
    }

    internal inline fun resolveOutline(
        size: Size,
        buildOutline: (halfW: Float, halfH: Float) -> Outline
    ): Outline {
        val halfW = size.width * 0.5f
        val halfH = size.height * 0.5f
        if (halfW <= 0f || halfH <= 0f || !halfW.isFinite() || !halfH.isFinite()) {
            return EmptyOutline
        }

        val cacheKey = (size.width.toRawBits().toLong() shl 32) or
            (size.height.toRawBits().toLong() and 0xFFFFFFFFL)

        synchronized(outlineCache) {
            outlineCache[cacheKey]?.let { return it }
        }

        val created = buildOutline(halfW, halfH)
        synchronized(outlineCache) {
            outlineCache[cacheKey] = created
        }
        return created
    }

    companion object {
        internal const val MAX_CACHED_OUTLINES = 64
        internal val EmptyOutline = Outline.Rectangle(Rect.Zero)

        val Icon = SquircleShape(exponent = 4.4f)
        val Card = SquircleShape(exponent = 5.2f)
    }
}

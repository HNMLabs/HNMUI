package com.hnmlabs.hnmui.ui.components

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

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val halfW = size.width / 2f
        val halfH = size.height / 2f
        if (halfW <= 0f || halfH <= 0f) {
            return Outline.Generic(Path())
        }

        val power = 2f / exponent
        val path = Path()

        for (i in 0 until steps) {
            val theta = (2.0 * PI * i) / steps
            val cosT = cos(theta).toFloat()
            val sinT = sin(theta).toFloat()

            val x = halfW + halfW * abs(cosT).pow(power).withSign(cosT)
            val y = halfH + halfH * abs(sinT).pow(power).withSign(sinT)

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        return Outline.Generic(path)
    }

    companion object {
        val Icon = SquircleShape(exponent = 4.4f)
        val Card = SquircleShape(exponent = 5.2f)
    }
}

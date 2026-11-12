package com.hnmlabs.hnmui.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * iOS'un doğal tepkiselliğini sağlayan yay fiziği (Spring Physics) sabitleri.
 * Doğrusal (linear/tween) animasyonlar yerine hız vektörünü koruyan sönümlü yaylar kullanılır.
 */
object HnmMotion {
    fun <T> sheetSpring() = spring<T>(
        dampingRatio = 0.84f,
        stiffness = 380f
    )

    fun <T> folderSpring() = spring<T>(
        dampingRatio = 0.80f,
        stiffness = 420f
    )

    fun <T> iconPressSpring() = spring<T>(
        dampingRatio = 0.66f,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> badgePopSpring() = spring<T>(
        dampingRatio = 0.58f,
        stiffness = 520f
    )
}

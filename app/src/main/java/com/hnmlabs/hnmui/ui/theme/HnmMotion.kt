package com.hnmlabs.hnmui.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntOffset

/**
 * iOS'un doğal tepkiselliğini sağlayan yay fiziği (Spring Physics) sabitleri.
 * Doğrusal (linear/tween) animasyonlar yerine hız vektörünü koruyan sönümlü yaylar kullanılır.
 */
object HnmMotion {
    /** Alt sayfa, Denetim Merkezi ve Bildirim Merkezi için sarsıntısız, sakin iOS yayı */
    fun <T> sheetSpring() = spring<T>(
        dampingRatio = 0.88f,
        stiffness = 380f
    )

    /** Piksel kaydırma (slideIn/slideOut) animasyonlarında alt-piksel takılmasını önleyen 1px eşikli yay */
    fun sheetOffsetSpring(): FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = 0.88f,
        stiffness = 380f,
        visibilityThreshold = IntOffset.VisibilityThreshold
    )

    /** Açık panel arkasındaki ana ekranın hafifçe geriye çekilme (depth recession) yayı */
    fun <T> recedeSpring() = spring<T>(
        dampingRatio = 0.90f,
        stiffness = 340f
    )

    /** Klasör açılış/kapanış ölçekleme yayı */
    fun <T> folderSpring() = spring<T>(
        dampingRatio = 0.86f,
        stiffness = 410f
    )

    /** İkon dokunma ve bırakma mikro tepki yayı */
    fun <T> iconPressSpring() = spring<T>(
        dampingRatio = 0.74f,
        stiffness = 460f
    )

    /** Sürüklenen ikonun parmak altında nazikçe yükselme yayı */
    fun <T> dragLiftSpring() = spring<T>(
        dampingRatio = 0.82f,
        stiffness = 460f
    )

    /** Izgara ve Dock hedef yuvası vurgu geçiş yayı */
    fun <T> slotHighlightSpring() = spring<T>(
        dampingRatio = 0.88f,
        stiffness = Spring.StiffnessMedium
    )

    /** Bildirim rozeti belirme yayı */
    fun <T> badgePopSpring() = spring<T>(
        dampingRatio = 0.64f,
        stiffness = 500f
    )
}

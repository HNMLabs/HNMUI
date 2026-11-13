package com.hnmlabs.hnmui.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.hnmlabs.hnmui.model.LiveNotificationCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

/**
 * Sıfır-polling prensibiyle çalışan canlı bildirim rozeti (Notification Badge) ve
 * Bildirim Merkezi kart akışı servisi.
 * Grup özeti (FLAG_GROUP_SUMMARY) ve kalıcı arka plan servis bildirimlerini filtreleyerek
 * gerçek okunmamış bildirim sayısını paket bazında yayınlar.
 */
class HnmNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instanceRef = WeakReference(this)
        _isServiceConnected.value = true
        recomputeBadges()
    }

    override fun onListenerDisconnected() {
        _isServiceConnected.value = false
        _badgeCounts.value = emptyMap()
        _activeCards.value = emptyList()
        instanceRef = null
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        recomputeBadges()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        recomputeBadges()
    }

    private fun recomputeBadges() {
        val activeList = runCatching { activeNotifications }.getOrNull() ?: return
        val counts = HashMap<String, Int>()
        val cards = ArrayList<LiveNotificationCard>()

        for (sbn in activeList) {
            val notification = sbn.notification ?: continue
            val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
            if (isGroupSummary) continue

            val pkg = sbn.packageName ?: continue
            if (!sbn.isOngoing) {
                val increment = notification.number.takeIf { it > 0 } ?: 1
                counts[pkg] = (counts[pkg] ?: 0) + increment
            }

            val extras = notification.extras
            val rawTitle = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
            val rawText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
                ?: extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()

            if (!rawTitle.isNullOrEmpty() || !rawText.isNullOrEmpty()) {
                cards += LiveNotificationCard(
                    key = sbn.key ?: "${pkg}_${sbn.tag.orEmpty()}_${sbn.id}_${sbn.postTime}",
                    packageName = pkg,
                    title = rawTitle ?: pkg,
                    message = rawText.orEmpty(),
                    postTimeMillis = sbn.postTime
                )
            }
        }

        _badgeCounts.value = counts
        _activeCards.value = cards
            .sortedByDescending { it.postTimeMillis }
            .distinctBy { it.key }
            .take(24)
    }

    companion object {
        private var instanceRef: WeakReference<HnmNotificationListenerService>? = null

        private val _badgeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
        val badgeCounts: StateFlow<Map<String, Int>> = _badgeCounts.asStateFlow()

        private val _activeCards = MutableStateFlow<List<LiveNotificationCard>>(emptyList())
        val activeCards: StateFlow<List<LiveNotificationCard>> = _activeCards.asStateFlow()

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        fun dismissNotificationByKey(key: String) {
            runCatching { instanceRef?.get()?.cancelNotification(key) }
        }

        fun dismissAllNotifications() {
            runCatching { instanceRef?.get()?.cancelAllNotifications() }
        }
    }
}

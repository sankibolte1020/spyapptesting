package com.security.testapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("hide_done", false)) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val hideIntent = Intent(context, HideReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 0, hideIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val trigger = System.currentTimeMillis() + 12 * 60 * 60 * 1000L
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
                    }
                } catch (_: Exception) {}
            }
        }
    }
}

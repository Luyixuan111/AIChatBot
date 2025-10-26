package com.example.chatbot

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class MedReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val name  = intent.getStringExtra("name") ?: "Medication"
        val dose  = intent.getStringExtra("dose") ?: ""
        val idStr = intent.getStringExtra("id") ?: System.currentTimeMillis().toString()
        val chan  = "meds_reminders"

        // Android 13+ needs POST_NOTIFICATIONS runtime permission
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val notif = NotificationCompat.Builder(ctx, chan)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Time to take: $name")
            .setContentText(if (dose.isBlank()) "Please take your medication." else dose)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val nm = NotificationManagerCompat.from(ctx)
        // (named args not allowed here) → use positional
        nm.notify(idStr.hashCode(), notif)

        // Repeat scheduling (if this reminder has repeat days)
        ReminderScheduler.scheduleNextFromIntent(ctx, intent)
    }
}

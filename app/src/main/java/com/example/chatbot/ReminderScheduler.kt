package com.example.chatbot

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object ReminderScheduler {

    fun scheduleOnce(ctx: Context, m: MedReminder) {
        val next = nextTriggerTime(m)
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next,
            pending(ctx, m)
        )
    }

    fun cancel(ctx: Context, m: MedReminder) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(ctx, m))
    }

    private fun pending(ctx: Context, m: MedReminder): PendingIntent {
        val i = Intent(ctx, MedReminderReceiver::class.java).apply {
            putExtra("id", m.id)
            putExtra("name", m.name)
            putExtra("dose", m.dose)
            putExtra("hour", m.hour)
            putExtra("minute", m.minute)
            putIntegerArrayListExtra("days", ArrayList(m.days))
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(ctx, m.id.hashCode(), i, flags)
    }

    fun scheduleNextFromIntent(ctx: Context, i: Intent) {
        val id = i.getStringExtra("id") ?: return
        val name = i.getStringExtra("name") ?: return
        val dose = i.getStringExtra("dose") ?: ""
        val hour = i.getIntExtra("hour", 9)
        val minute = i.getIntExtra("minute", 0)
        val days = i.getIntegerArrayListExtra("days")?.toSet() ?: emptySet()
        if (days.isEmpty()) return  // one-time reminder; do not reschedule

        val m = MedReminder(id, name, dose, hour, minute, days)
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTriggerTime(m),
            pending(ctx, m)
        )
    }

    private fun nextTriggerTime(m: MedReminder): Long {
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, m.hour)
            set(Calendar.MINUTE, m.minute)
        }

        fun mon1Sun7(c: Calendar): Int = ((c.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
        // Java Calendar: SUN=1..SAT=7 → 1=Mon..7=Sun

        if (m.days.isEmpty()) {
            if (cal.timeInMillis <= now.timeInMillis) cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }

        for (d in 0..7) {
            val tmp = cal.clone() as Calendar
            tmp.add(Calendar.DAY_OF_YEAR, d)
            if (mon1Sun7(tmp) in m.days && tmp.timeInMillis > now.timeInMillis) {
                return tmp.timeInMillis
            }
        }
        cal.add(Calendar.DAY_OF_YEAR, 7)
        return cal.timeInMillis
    }
}

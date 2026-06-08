package com.protocol.alerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.protocol.alerts.model.AlertEvent
import com.protocol.alerts.model.Schedule
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The reliability core. Resolves today's day type and (re)schedules one exact alarm per event.
 * Idempotent: cancels all known event alarms first, so it can run on boot / daily / on edit safely.
 */
object DailyPlanner {
    const val ACTION_FIRE = "com.protocol.alerts.FIRE"
    const val EXTRA_EVENT_ID = "event_id"
    const val EXTRA_TITLE = "title"
    const val EXTRA_BODY = "body"
    const val EXTRA_CHANNEL = "channel"

    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun resolveType(schedule: Schedule, prefs: PrefsStore, date: LocalDate): String {
        prefs.overrideForDate(date.format(dateFmt))?.let { return it }
        val weekday = date.dayOfWeek.name // MONDAY ...
        val default = schedule.weekdayDefault[weekday] ?: "REST"
        return prefs.dayTypeFor(weekday, default)
    }

    fun todaysEvents(context: Context): Pair<String, List<AlertEvent>> {
        val schedule = ScheduleRepository.load(context)
        val type = resolveType(schedule, PrefsStore(context), LocalDate.now())
        return type to (schedule.days[type]?.events ?: emptyList())
    }

    fun planToday(context: Context) {
        val schedule = ScheduleRepository.load(context)
        val prefs = PrefsStore(context)
        val am = context.getSystemService(AlarmManager::class.java)
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()

        // Cancel every known event alarm (idempotent reset).
        schedule.days.values.flatMap { it.events }.distinctBy { it.id }.forEach { ev ->
            am.cancel(firePendingIntent(context, ev.id, null, null))
        }
        if (!prefs.masterEnabled) return

        val type = resolveType(schedule, prefs, today)
        val day = schedule.days[type] ?: return
        val now = System.currentTimeMillis()

        for (ev in day.events) {
            val t = LocalTime.parse(ev.time)
            val triggerMillis = today.atTime(t)
                .minusMinutes(ev.leadMinutes.toLong())
                .atZone(zone).toInstant().toEpochMilli()
            if (triggerMillis <= now) continue

            val pi = firePendingIntent(context, ev.id, ev, day.channel)
            try {
                if (am.canScheduleExactAlarmsCompat()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
                }
            } catch (e: SecurityException) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
            }
        }
    }

    private fun firePendingIntent(
        context: Context, eventId: String, ev: AlertEvent?, channel: String?
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtra(EXTRA_EVENT_ID, eventId)
            ev?.let { putExtra(EXTRA_TITLE, it.title); putExtra(EXTRA_BODY, it.body) }
            channel?.let { putExtra(EXTRA_CHANNEL, it) }
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, eventId.hashCode(), intent, flags)
    }
}

fun AlarmManager.canScheduleExactAlarmsCompat(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) canScheduleExactAlarms() else true

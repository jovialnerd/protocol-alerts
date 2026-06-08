package com.protocol.alerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DONE -> {
                NotificationManagerCompat.from(context).cancel(intent.getIntExtra(EXTRA_NID, 0))
                return
            }
            ACTION_SNOOZE -> {
                scheduleSnooze(context, intent)
                NotificationManagerCompat.from(context).cancel(intent.getIntExtra(EXTRA_NID, 0))
                return
            }
        }
        // Default: an event fired -> post its notification.
        NotificationChannels.ensure(context)
        val eventId = intent.getStringExtra(DailyPlanner.EXTRA_EVENT_ID) ?: return
        val title = intent.getStringExtra(DailyPlanner.EXTRA_TITLE) ?: "Protocol reminder"
        val body = intent.getStringExtra(DailyPlanner.EXTRA_BODY) ?: ""
        val channel = intent.getStringExtra(DailyPlanner.EXTRA_CHANNEL) ?: NotificationChannels.TRAINING
        post(context, eventId, title, body, channel)
    }

    private fun scheduleSnooze(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(DailyPlanner.EXTRA_EVENT_ID) ?: return
        val am = context.getSystemService(AlarmManager::class.java)
        val refire = Intent(context, AlarmReceiver::class.java).apply {
            action = DailyPlanner.ACTION_FIRE
            putExtra(DailyPlanner.EXTRA_EVENT_ID, eventId)
            putExtra(DailyPlanner.EXTRA_TITLE, intent.getStringExtra(DailyPlanner.EXTRA_TITLE))
            putExtra(DailyPlanner.EXTRA_BODY, intent.getStringExtra(DailyPlanner.EXTRA_BODY))
            putExtra(DailyPlanner.EXTRA_CHANNEL, intent.getStringExtra(DailyPlanner.EXTRA_CHANNEL))
        }
        val pi = PendingIntent.getBroadcast(
            context, (eventId + "_snooze_fire").hashCode(), refire,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val at = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L
        try {
            if (am.canScheduleExactAlarmsCompat())
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    companion object {
        const val ACTION_DONE = "com.protocol.alerts.DONE"
        const val ACTION_SNOOZE = "com.protocol.alerts.SNOOZE"
        const val EXTRA_NID = "nid"
        const val SNOOZE_MINUTES = 10L

        fun post(context: Context, eventId: String, title: String, body: String, channel: String) {
            val nid = eventId.hashCode()

            val openPi = PendingIntent.getActivity(
                context, nid,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            fun action(act: String, reqSuffix: String): PendingIntent {
                val i = Intent(context, AlarmReceiver::class.java).apply {
                    action = act
                    putExtra(EXTRA_NID, nid)
                    putExtra(DailyPlanner.EXTRA_EVENT_ID, eventId)
                    putExtra(DailyPlanner.EXTRA_TITLE, title)
                    putExtra(DailyPlanner.EXTRA_BODY, body)
                    putExtra(DailyPlanner.EXTRA_CHANNEL, channel)
                }
                return PendingIntent.getBroadcast(
                    context, (eventId + reqSuffix).hashCode(), i,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            val n = NotificationCompat.Builder(context, channel)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(openPi)
                .addAction(0, "Done", action(ACTION_DONE, "_done"))
                .addAction(0, "Snooze 10m", action(ACTION_SNOOZE, "_snooze"))
                .build()

            NotificationManagerCompat.from(context).notify(nid, n)
        }
    }
}

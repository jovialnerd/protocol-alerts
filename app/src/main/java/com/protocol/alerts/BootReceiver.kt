package com.protocol.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-arm alarms after reboot, app update, or time/timezone change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationChannels.ensure(context)
        DailyPlanner.planToday(context)
    }
}

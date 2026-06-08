package com.protocol.alerts

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensure(this)
        DailyRefreshWorker.schedule(this)
        DailyPlanner.planToday(this)
    }
}

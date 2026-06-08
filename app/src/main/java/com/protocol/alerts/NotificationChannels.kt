package com.protocol.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/** Three channels so each day type can have its own importance / sound. minSdk 26 => always available. */
object NotificationChannels {
    const val TRAINING = "training"
    const val MATCH = "match"
    const val REST = "rest"

    fun ensure(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        fun make(id: String, name: String, importance: Int) {
            val ch = NotificationChannel(id, name, importance).apply {
                description = "Protocol alerts — $name"
                enableVibration(true)
            }
            nm.createNotificationChannel(ch)
        }
        make(TRAINING, "Training Day", NotificationManager.IMPORTANCE_HIGH)
        make(MATCH, "Match Day", NotificationManager.IMPORTANCE_HIGH)
        make(REST, "Rest Day", NotificationManager.IMPORTANCE_DEFAULT)
    }

    fun channelFor(type: String): String = when (type.uppercase()) {
        "MATCH" -> MATCH
        "REST" -> REST
        else -> TRAINING
    }
}

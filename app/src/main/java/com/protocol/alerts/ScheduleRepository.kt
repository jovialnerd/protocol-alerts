package com.protocol.alerts

import android.content.Context
import com.protocol.alerts.model.Schedule
import kotlinx.serialization.json.Json

object ScheduleRepository {
    private val json = Json { ignoreUnknownKeys = true }
    @Volatile private var cached: Schedule? = null

    fun load(context: Context): Schedule {
        cached?.let { return it }
        val text = context.assets.open("schedule.json")
            .bufferedReader().use { it.readText() }
        return json.decodeFromString(Schedule.serializer(), text).also { cached = it }
    }
}

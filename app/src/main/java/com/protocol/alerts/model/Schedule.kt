package com.protocol.alerts.model

import kotlinx.serialization.Serializable

@Serializable
data class Schedule(
    val version: String = "",
    val source: String = "",
    val weekdayDefault: Map<String, String> = emptyMap(),
    val days: Map<String, DayPlan> = emptyMap()
)

@Serializable
data class DayPlan(
    val channel: String = "training",
    val label: String = "",
    val events: List<AlertEvent> = emptyList()
)

@Serializable
data class AlertEvent(
    val id: String,
    val time: String,        // "HH:mm" local wall-clock
    val leadMinutes: Int = 0,
    val title: String,
    val body: String = ""
)

package com.protocol.alerts

import android.content.Context

/** Synchronous SharedPreferences wrapper (safe to read from BroadcastReceivers). */
class PrefsStore(context: Context) {
    private val sp = context.getSharedPreferences("protocol_prefs", Context.MODE_PRIVATE)

    var masterEnabled: Boolean
        get() = sp.getBoolean("master_enabled", true)
        set(v) = sp.edit().putBoolean("master_enabled", v).apply()

    /** Per-weekday assigned type, falling back to the bundled default. weekday e.g. "MONDAY". */
    fun dayTypeFor(weekday: String, default: String): String =
        sp.getString("wd_$weekday", null) ?: default

    fun setDayType(weekday: String, type: String) =
        sp.edit().putString("wd_$weekday", type).apply()

    /** One-off override for a specific date (yyyy-MM-dd). */
    fun overrideForDate(date: String): String? = sp.getString("ov_$date", null)

    fun setOverrideForDate(date: String, type: String?) {
        sp.edit().apply { if (type == null) remove("ov_$date") else putString("ov_$date", type) }.apply()
    }
}

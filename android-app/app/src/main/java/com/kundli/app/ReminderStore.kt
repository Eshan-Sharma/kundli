package com.kundli.app

import android.content.Context
import org.json.JSONArray
import java.util.Calendar

/**
 * Reads the reminders mirrored from the WebView (via KundliBridge → SharedPreferences)
 * and matches them against today's panchang. Used by the reminder widgets and by
 * DailyWorker for daily notifications.
 */
object ReminderStore {

    data class Rule(
        val name: String,
        val message: String,
        val active: Boolean,
        val tithi: Int?,
        val paksha: String?,
        val vaar: Int?,
        val nakshatra: Int?,
        val month: String?,
        val date: String?
    )

    fun load(ctx: Context): List<Rule> {
        val json = ctx.getSharedPreferences("kundli", Context.MODE_PRIVATE)
            .getString("reminders", "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            val out = mutableListOf<Rule>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(Rule(
                    name = o.optString("name"),
                    message = o.optString("message", ""),
                    active = o.optBoolean("active", true),
                    tithi = if (o.isNull("tithi")) null else o.optInt("tithi"),
                    paksha = if (o.isNull("paksha")) null else o.optString("paksha", null),
                    vaar = if (o.isNull("vaar")) null else o.optInt("vaar"),
                    nakshatra = if (o.isNull("nakshatra")) null else o.optInt("nakshatra"),
                    month = if (o.isNull("month")) null else o.optString("month", null),
                    date = if (o.isNull("date")) null else o.optString("date", null)
                ))
            }
            out
        } catch (e: Exception) { emptyList() }
    }

    /** Returns the user reminders that fire on `today` based on panchang `pan`. */
    fun matchesToday(rules: List<Rule>, pan: PanchangCalc.Panchang, today: Calendar): List<Rule> {
        val wpTithi = (pan.tithiNum % 15) + 1
        val y = today.get(Calendar.YEAR)
        val m = today.get(Calendar.MONTH) + 1
        val d = today.get(Calendar.DAY_OF_MONTH)
        val ymd = String.format("%04d-%02d-%02d", y, m, d)
        val mmdd = String.format("%02d-%02d", m, d)
        return rules.filter { r ->
            if (!r.active) return@filter false
            if (r.tithi != null && r.tithi != wpTithi) return@filter false
            if (r.paksha != null && r.paksha != "any" && r.paksha != pan.paksha) return@filter false
            if (r.vaar != null && r.vaar != pan.weekday) return@filter false
            if (r.nakshatra != null && r.nakshatra != pan.nakshatra.idx) return@filter false
            if (r.month != null && r.month != pan.hinduMonth) return@filter false
            if (r.date != null) {
                when (r.date.length) {
                    10 -> if (r.date != ymd) return@filter false
                    5 -> if (r.date != mmdd) return@filter false
                }
            }
            // at least one condition must be set
            val anyCond = r.tithi != null || r.vaar != null || r.nakshatra != null ||
                r.month != null || r.date != null ||
                (r.paksha != null && r.paksha != "any")
            anyCond
        }
    }
}

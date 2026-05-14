package com.kundli.app

import java.util.Calendar
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/**
 * Self-contained Kotlin port of the moon-direction calculator from kundli.html.
 *
 * Pipeline:
 *   weekday + date  →  Julian Day  →  Moon's tropical longitude (Brown's lunar theory, top terms)
 *   →  minus Lahiri ayanamsa  →  sidereal longitude  →  sign  →  cardinal direction
 *   Disha Shool blocks one direction per weekday.
 */
object DirectionCalc {

    private val SIGNS = arrayOf(
        "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
        "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
    )
    // Fire → E, Earth → S, Air → W, Water → N
    private val MOON_DIR_BY_SIGN = arrayOf("E", "S", "W", "N", "E", "S", "W", "N", "E", "S", "W", "N")
    // Disha Shool by weekday: 0=Sun … 6=Sat
    private val DISHA_SHOOL = arrayOf("W", "E", "N", "N", "S", "W", "E")
    private val WEEKDAY_NAMES = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    private val WEEKDAY_FULL = arrayOf("Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday")
    private val MONTH_NAMES = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    // Clockwise-right of facing direction (= "right of" the moon)
    private val DIR_RIGHT = mapOf("N" to "E", "E" to "S", "S" to "W", "W" to "N")
    val DIR_NAME = mapOf("N" to "North", "E" to "East", "S" to "South", "W" to "West")
    val DIR_ARROW = mapOf("N" to "↑", "E" to "→", "S" to "↓", "W" to "←")

    data class DirInfo(
        val dateLabel: String,       // "14 May 2026"
        val weekdayShort: String,    // "Thu"
        val weekdayFull: String,     // "Thursday"
        val moonSign: String,        // "Pisces"
        val moonDir: String,         // "N"
        val primary: String,         // "N"
        val secondary: String,       // "E"
        val primaryBlocked: Boolean,
        val secondaryBlocked: Boolean,
        val blocked: String,         // "S"
        val good: List<String>       // ["N","E"]
    )

    fun computeFor(cal: Calendar): DirInfo {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val tzOffsetMillis = cal.timeZone.getOffset(cal.timeInMillis)
        val tzHours = tzOffsetMillis / 3600000.0

        // Noon local → noon UT = 12 - tz
        val jd = julianDay(y, m, d, 12.0 - tzHours)
        val ay = ayanamsa(jd)
        val moonSid = mod360(moonLongitude(jd) - ay)
        val moonSign = floor(moonSid / 30.0).toInt().coerceIn(0, 11)
        val moonDir = MOON_DIR_BY_SIGN[moonSign]

        // Calendar.DAY_OF_WEEK: 1=Sun … 7=Sat → 0..6
        val weekday = cal.get(Calendar.DAY_OF_WEEK) - 1
        val blocked = DISHA_SHOOL[weekday]

        val primary = moonDir
        val secondary = DIR_RIGHT[moonDir]!!
        val primaryBlocked = primary == blocked
        val secondaryBlocked = secondary == blocked

        val good = mutableListOf<String>()
        if (!primaryBlocked) good.add(primary)
        if (!secondaryBlocked && !good.contains(secondary)) good.add(secondary)

        return DirInfo(
            dateLabel = "$d ${MONTH_NAMES[m - 1]} $y",
            weekdayShort = WEEKDAY_NAMES[weekday],
            weekdayFull = WEEKDAY_FULL[weekday],
            moonSign = SIGNS[moonSign],
            moonDir = moonDir,
            primary = primary,
            secondary = secondary,
            primaryBlocked = primaryBlocked,
            secondaryBlocked = secondaryBlocked,
            blocked = blocked,
            good = good
        )
    }

    // ---------- ASTRO CORE ----------

    private fun julianDay(year: Int, month: Int, day: Int, hour: Double): Double {
        var y = year; var m = month
        if (m <= 2) { y -= 1; m += 12 }
        val A = floor(y / 100.0)
        val B = 2 - A + floor(A / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + B - 1524.5 + hour / 24.0
    }

    /** Lahiri ayanamsa polynomial — same as in the HTML file. */
    private fun ayanamsa(jd: Double): Double {
        val T = (jd - 2451545.0) / 36525.0
        return 23.85 + 0.0137 * T * 100.0 + 0.000308 * T * T
    }

    /** Moon's tropical longitude using Brown's lunar theory (top terms). */
    private fun moonLongitude(jd: Double): Double {
        val T = (jd - 2451545.0) / 36525.0
        val L  = mod360(218.3164477 + 481267.88123421 * T - 0.0015786 * T * T)
        val D  = mod360(297.8501921 + 445267.1114034  * T)
        val M  = mod360(357.5291092 +  35999.0502909  * T)
        val Mp = mod360(134.9633964 + 477198.8675055  * T)
        val F  = mod360( 93.2720950 + 483202.0175233  * T)

        var dl = 0.0
        dl +=  6.288774 * sind(Mp)
        dl +=  1.274027 * sind(2*D - Mp)
        dl +=  0.658314 * sind(2*D)
        dl +=  0.213618 * sind(2*Mp)
        dl += -0.185116 * sind(M)
        dl += -0.114332 * sind(2*F)
        dl +=  0.058793 * sind(2*D - 2*Mp)
        dl +=  0.057066 * sind(2*D - M - Mp)
        dl +=  0.053322 * sind(2*D + Mp)
        dl +=  0.045758 * sind(2*D - M)
        dl += -0.040923 * sind(M - Mp)
        dl += -0.034720 * sind(D)
        dl += -0.030383 * sind(M + Mp)
        return mod360(L + dl)
    }

    private fun sind(x: Double) = sin(x * PI / 180.0)
    private fun mod360(x: Double): Double { var r = x % 360.0; if (r < 0) r += 360.0; return r }
}

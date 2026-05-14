package com.kundli.app

import java.util.Calendar
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.asin

/**
 * Comprehensive Panchang calculator — Kotlin port of computePanchang from index.html.
 * Used by widgets so they don't need a WebView. Lahiri ayanamsa, Brown's lunar theory (top terms).
 */
object PanchangCalc {

    val SIGNS = arrayOf("Aries","Taurus","Gemini","Cancer","Leo","Virgo","Libra","Scorpio","Sagittarius","Capricorn","Aquarius","Pisces")
    val WEEKDAY_FULL = arrayOf("Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday")
    val WEEKDAY_SHORT = arrayOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")
    val MONTH_NAMES = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    val MOON_DIR_BY_SIGN = arrayOf("E","S","W","N","E","S","W","N","E","S","W","N")
    val DISHA_SHOOL = arrayOf("W","E","N","N","S","W","E")
    private val DIR_RIGHT = mapOf("N" to "E", "E" to "S", "S" to "W", "W" to "N")
    val DIR_NAME = mapOf("N" to "North", "E" to "East", "S" to "South", "W" to "West")

    val TITHI_NAMES = arrayOf(
        "Pratipada","Dwitiya","Tritiya","Chaturthi","Panchami","Shashthi","Saptami","Ashtami","Navami","Dashami","Ekadashi","Dwadashi","Trayodashi","Chaturdashi","Purnima",
        "Pratipada","Dwitiya","Tritiya","Chaturthi","Panchami","Shashthi","Saptami","Ashtami","Navami","Dashami","Ekadashi","Dwadashi","Trayodashi","Chaturdashi","Amavasya"
    )
    val YOGA_NAMES = arrayOf("Vishkambha","Preeti","Ayushman","Saubhagya","Shobhana","Atiganda","Sukarma","Dhriti","Shoola","Ganda","Vriddhi","Dhruva","Vyaghata","Harshana","Vajra","Siddhi","Vyatipata","Variyana","Parigha","Shiva","Siddha","Sadhya","Shubha","Shukla","Brahma","Indra","Vaidhriti")
    val NAKSHATRAS = arrayOf(
        "Ashwini","Bharani","Krittika","Rohini","Mrigashira","Ardra","Punarvasu","Pushya","Ashlesha",
        "Magha","Purva Phalguni","Uttara Phalguni","Hasta","Chitra","Swati","Vishakha","Anuradha","Jyeshtha",
        "Mula","Purvashadha","Uttarashadha","Shravana","Dhanishta","Shatabhisha","Purva Bhadrapada","Uttara Bhadrapada","Revati"
    )
    val HINDU_MONTH = arrayOf("Vaishakha","Jyeshtha","Ashadha","Shravana","Bhadrapada","Ashwina","Kartika","Margashirsha","Pausha","Magha","Phalguna","Chaitra")
    val RITU = arrayOf("Vasanta","Vasanta","Grishma","Grishma","Varsha","Varsha","Sharad","Sharad","Hemanta","Hemanta","Shishira","Shishira")

    // Choghadia
    val CHOG_DAY_START = arrayOf("Udveg","Amrit","Rog","Labh","Shubh","Char","Kaal")
    val CHOG_NIGHT_START = arrayOf("Shubh","Char","Kaal","Udveg","Amrit","Rog","Labh")
    val CHOG_SEQ = arrayOf("Udveg","Char","Labh","Amrit","Kaal","Shubh","Rog")
    val CHOG_TYPE = mapOf("Udveg" to "bad","Char" to "neutral","Labh" to "good","Amrit" to "good","Kaal" to "bad","Shubh" to "good","Rog" to "bad")

    val RAHU_SLOT = intArrayOf(8, 2, 7, 5, 6, 4, 3)
    val YAMA_SLOT = intArrayOf(4, 3, 2, 1, 7, 6, 5)
    val GULIKA_SLOT = intArrayOf(7, 6, 5, 4, 3, 2, 1)

    data class TimeSlot(val startHour: Double, val endHour: Double)
    data class SunTimes(val sunrise: Double, val sunset: Double, val solarNoon: Double, val dayLength: Double)
    data class Nakshatra(val idx: Int, val name: String, val pada: Int)
    data class Choghadia(val name: String, val type: String, val start: Double, val end: Double)
    data class Festival(val en: String, val hi: String)

    data class Panchang(
        val date: Calendar,
        val weekday: Int,
        val tithiNum: Int,           // 0..29 (Shukla 0-14, Krishna 15-29)
        val tithiName: String,
        val paksha: String,          // "Shukla" | "Krishna"
        val nakshatra: Nakshatra,
        val yogaName: String,
        val karanaName: String,
        val hinduMonth: String,
        val ritu: String,
        val moonSign: Int,
        val sunSign: Int,
        val moonDir: String,
        val blocked: String,         // Disha Shool
        val primary: String,
        val secondary: String,
        val sun: SunTimes,
        val rahu: TimeSlot,
        val yama: TimeSlot,
        val gulika: TimeSlot,
        val abhijit: TimeSlot,
        val brahma: TimeSlot,
        val dayChoghadia: List<Choghadia>,
        val nightChoghadia: List<Choghadia>
    )

    fun computeFor(cal: Calendar, lat: Double, lon: Double): Panchang {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val tz = cal.timeZone.getOffset(cal.timeInMillis) / 3600000.0

        val jdNoonUT = julianDay(y, m, d, 12.0 - tz)
        val sunTrop = sunLongitude(jdNoonUT)
        val moonTrop = moonLongitude(jdNoonUT)
        val ay = ayanamsa(jdNoonUT)
        val sunSid = mod360(sunTrop - ay)
        val moonSid = mod360(moonTrop - ay)

        val diff = mod360(moonTrop - sunTrop)
        val tithiNum = floor(diff / 12.0).toInt().coerceIn(0, 29)
        val paksha = if (tithiNum < 15) "Shukla" else "Krishna"
        val tithiName = TITHI_NAMES[tithiNum]

        val nakIdx = floor(moonSid * 27.0 / 360.0).toInt().coerceIn(0, 26)
        val pada = (floor((moonSid * 27.0 / 360.0 - nakIdx) * 4.0).toInt() + 1).coerceIn(1, 4)
        val nakshatra = Nakshatra(nakIdx, NAKSHATRAS[nakIdx], pada)

        val yogaSum = mod360(sunTrop + moonTrop)
        val yogaNum = floor(yogaSum * 27.0 / 360.0).toInt().coerceIn(0, 26)
        val yogaName = YOGA_NAMES[yogaNum]

        val karanaNum = floor(diff / 6.0).toInt().coerceIn(0, 59)
        val karanaName = karanaForNum(karanaNum)

        val sunSign = floor(sunSid / 30.0).toInt().coerceIn(0, 11)
        val moonSign = floor(moonSid / 30.0).toInt().coerceIn(0, 11)
        val hinduMonth = HINDU_MONTH[sunSign]
        val ritu = RITU[sunSign]

        val weekday = cal.get(Calendar.DAY_OF_WEEK) - 1
        val moonDir = MOON_DIR_BY_SIGN[moonSign]
        val blocked = DISHA_SHOOL[weekday]
        val primary = moonDir
        val secondary = DIR_RIGHT[moonDir]!!

        val sun = sunRiseSet(y, m, d, lat, lon, tz)
        val dayLen = sun.dayLength
        val rahu   = kaalSlot(RAHU_SLOT[weekday],   sun.sunrise, dayLen)
        val yama   = kaalSlot(YAMA_SLOT[weekday],   sun.sunrise, dayLen)
        val gulika = kaalSlot(GULIKA_SLOT[weekday], sun.sunrise, dayLen)
        val muhurtLen = dayLen / 15.0
        val abhijit = TimeSlot(addHrs(sun.solarNoon, -muhurtLen / 2.0), addHrs(sun.solarNoon, muhurtLen / 2.0))
        val brahma = TimeSlot(addHrs(sun.sunrise, -96.0 / 60.0), addHrs(sun.sunrise, -48.0 / 60.0))

        val dayChog = mutableListOf<Choghadia>()
        var cur = CHOG_DAY_START[weekday]
        val dSlot = dayLen / 8.0
        for (i in 0 until 8) {
            dayChog.add(Choghadia(cur, CHOG_TYPE[cur] ?: "neutral",
                addHrs(sun.sunrise, dSlot * i), addHrs(sun.sunrise, dSlot * (i + 1))))
            cur = CHOG_SEQ[(CHOG_SEQ.indexOf(cur) + 1) % 7]
        }
        val nightLen = 24.0 - dayLen
        val nSlot = nightLen / 8.0
        val nightChog = mutableListOf<Choghadia>()
        cur = CHOG_NIGHT_START[weekday]
        for (i in 0 until 8) {
            nightChog.add(Choghadia(cur, CHOG_TYPE[cur] ?: "neutral",
                addHrs(sun.sunset, nSlot * i), addHrs(sun.sunset, nSlot * (i + 1))))
            cur = CHOG_SEQ[(CHOG_SEQ.indexOf(cur) + 1) % 7]
        }

        return Panchang(cal, weekday, tithiNum, tithiName, paksha, nakshatra,
            yogaName, karanaName, hinduMonth, ritu, moonSign, sunSign,
            moonDir, blocked, primary, secondary, sun, rahu, yama, gulika, abhijit, brahma,
            dayChog, nightChog)
    }

    fun dateLabel(cal: Calendar): String {
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val m = cal.get(Calendar.MONTH)
        val y = cal.get(Calendar.YEAR)
        return "$d ${MONTH_NAMES[m]} $y"
    }

    fun fmtTime(h: Double): String {
        if (h.isNaN()) return "—"
        val hh = floor(h).toInt()
        val mm = floor((h - hh) * 60.0).toInt()
        return "%02d:%02d".format(hh, mm)
    }

    private fun karanaForNum(k: Int): String {
        val mov = arrayOf("Bava","Balava","Kaulava","Taitila","Garaja","Vanija","Vishti")
        val fix = arrayOf("Shakuni","Chatushpada","Naga","Kimstughna")
        return if (k <= 55) mov[k % 7] else fix[k - 56]
    }

    private fun kaalSlot(slot: Int, sunrise: Double, dayLen: Double): TimeSlot {
        val len = dayLen / 8.0
        return TimeSlot(addHrs(sunrise, len * (slot - 1)), addHrs(sunrise, len * slot))
    }
    private fun addHrs(h: Double, add: Double): Double {
        var r = h + add
        while (r < 0) r += 24
        while (r >= 24) r -= 24
        return r
    }

    // ---------- Astro core ----------
    fun julianDay(year: Int, month: Int, day: Int, hour: Double): Double {
        var y = year; var m = month
        if (m <= 2) { y -= 1; m += 12 }
        val A = floor(y / 100.0)
        val B = 2 - A + floor(A / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + B - 1524.5 + hour / 24.0
    }
    fun ayanamsa(jd: Double): Double {
        val T = (jd - 2451545.0) / 36525.0
        return 23.85 + 0.0137 * T * 100.0 + 0.000308 * T * T
    }
    fun sunLongitude(jd: Double): Double {
        val n = jd - 2451545.0
        val L = mod360(280.460 + 0.9856474 * n)
        val g = mod360(357.528 + 0.9856003 * n)
        return mod360(L + 1.915 * sind(g) + 0.020 * sind(2 * g))
    }
    fun moonLongitude(jd: Double): Double {
        val T = (jd - 2451545.0) / 36525.0
        val L = mod360(218.3164477 + 481267.88123421 * T - 0.0015786 * T * T)
        val D = mod360(297.8501921 + 445267.1114034 * T)
        val M = mod360(357.5291092 + 35999.0502909 * T)
        val Mp = mod360(134.9633964 + 477198.8675055 * T)
        val F = mod360(93.2720950 + 483202.0175233 * T)
        var dl = 6.288774 * sind(Mp)
        dl += 1.274027 * sind(2*D - Mp)
        dl += 0.658314 * sind(2*D)
        dl += 0.213618 * sind(2*Mp)
        dl += -0.185116 * sind(M)
        dl += -0.114332 * sind(2*F)
        dl += 0.058793 * sind(2*D - 2*Mp)
        dl += 0.057066 * sind(2*D - M - Mp)
        dl += 0.053322 * sind(2*D + Mp)
        dl += 0.045758 * sind(2*D - M)
        dl += -0.040923 * sind(M - Mp)
        dl += -0.034720 * sind(D)
        dl += -0.030383 * sind(M + Mp)
        return mod360(L + dl)
    }
    fun sunRiseSet(year: Int, month: Int, day: Int, lat: Double, lon: Double, tz: Double): SunTimes {
        val jd0 = julianDay(year, month, day, 0.0)
        val nJ = Math.round(jd0 - 2451545.0 + 0.0008).toDouble()
        val Jstar = nJ - lon / 360.0
        val M = mod360(357.5291 + 0.98560028 * Jstar)
        val C = 1.9148 * sind(M) + 0.0200 * sind(2 * M) + 0.0003 * sind(3 * M)
        val lambda = mod360(M + C + 180 + 102.9372)
        val Jtransit = 2451545.0 + Jstar + 0.0053 * sind(M) - 0.0069 * sind(2 * lambda)
        val delta = asin(sind(lambda) * sind(23.44)) / DEG
        val cosH = (sind(-0.83) - sind(lat) * sind(delta)) / (cosd(lat) * cosd(delta))
        if (cosH > 1 || cosH < -1) return SunTimes(Double.NaN, Double.NaN, Double.NaN, 0.0)
        val H = acos(cosH) / DEG
        val Jset = Jtransit + H / 360.0
        val Jrise = Jtransit - H / 360.0
        return SunTimes(jdToLocalHour(Jrise, tz), jdToLocalHour(Jset, tz), jdToLocalHour(Jtransit, tz), (Jset - Jrise) * 24.0)
    }
    private fun jdToLocalHour(jd: Double, tz: Double): Double {
        val utHour = ((jd + 0.5) - floor(jd + 0.5)) * 24.0
        var h = utHour + tz
        while (h < 0) h += 24.0
        while (h >= 24) h -= 24.0
        return h
    }

    private const val DEG = PI / 180.0
    private fun sind(x: Double) = sin(x * DEG)
    private fun cosd(x: Double) = cos(x * DEG)
    private fun mod360(x: Double): Double { var r = x % 360.0; if (r < 0) r += 360.0; return r }

    // ---------- Festival lookup ----------
    private val FESTIVALS = listOf(
        Triple("Shukla|1|Chaitra",       "Gudi Padwa / Hindu New Year",  "गुड़ी पड़वा"),
        Triple("Shukla|9|Chaitra",       "Ram Navami",                   "राम नवमी"),
        Triple("Shukla|15|Chaitra",      "Hanuman Jayanti",              "हनुमान जयंती"),
        Triple("Shukla|3|Vaishakha",     "Akshaya Tritiya",              "अक्षय तृतीया"),
        Triple("Shukla|15|Vaishakha",    "Buddha Purnima",               "बुद्ध पूर्णिमा"),
        Triple("Shukla|10|Jyeshtha",     "Ganga Dussehra",               "गंगा दशहरा"),
        Triple("Krishna|15|Jyeshtha",    "Vat Savitri Vrat",             "वट सावित्री व्रत"),
        Triple("Shukla|2|Ashadha",       "Jagannath Rath Yatra",         "जगन्नाथ रथ यात्रा"),
        Triple("Shukla|15|Ashadha",      "Guru Purnima",                 "गुरु पूर्णिमा"),
        Triple("Krishna|15|Ashadha",     "Hariyali Amavasya",            "हरियाली अमावस्या"),
        Triple("Shukla|15|Bhadrapada",   "Raksha Bandhan",               "रक्षा बंधन"),
        Triple("Krishna|8|Bhadrapada",   "Krishna Janmashtami",          "कृष्ण जन्माष्टमी"),
        Triple("Shukla|4|Bhadrapada",    "Ganesh Chaturthi",             "गणेश चतुर्थी"),
        Triple("Shukla|1|Ashwina",       "Sharad Navratri begins",       "शारदीय नवरात्रि"),
        Triple("Shukla|8|Ashwina",       "Maha Ashtami",                 "महाष्टमी"),
        Triple("Shukla|9|Ashwina",       "Maha Navami",                  "महानवमी"),
        Triple("Shukla|10|Ashwina",      "Dussehra / Vijaya Dashami",    "विजयदशमी"),
        Triple("Shukla|15|Ashwina",      "Sharad Purnima",               "शरद पूर्णिमा"),
        Triple("Krishna|4|Kartika",      "Karwa Chauth",                 "करवा चौथ"),
        Triple("Krishna|13|Kartika",     "Dhanteras",                    "धनतेरस"),
        Triple("Krishna|14|Kartika",     "Naraka Chaturdashi",           "नरक चतुर्दशी"),
        Triple("Krishna|15|Kartika",     "Diwali / Lakshmi Puja",        "दीपावली"),
        Triple("Shukla|1|Kartika",       "Govardhan Puja",               "गोवर्धन पूजा"),
        Triple("Shukla|2|Kartika",       "Bhai Dooj",                    "भाई दूज"),
        Triple("Shukla|6|Kartika",       "Chhath Puja",                  "छठ पूजा"),
        Triple("Shukla|15|Kartika",      "Kartik Purnima",               "कार्तिक पूर्णिमा"),
        Triple("Shukla|11|Margashirsha", "Mokshada Ekadashi",            "मोक्षदा एकादशी"),
        Triple("Shukla|5|Magha",         "Vasant Panchami",              "वसंत पंचमी"),
        Triple("Shukla|15|Magha",        "Magha Purnima",                "माघ पूर्णिमा"),
        Triple("Krishna|13|Magha",       "Maha Shivaratri",              "महाशिवरात्रि"),
        Triple("Shukla|15|Phalguna",     "Holika Dahan",                 "होलिका दहन"),
        Triple("Krishna|1|Phalguna",     "Holi",                         "होली"),
        Triple("Krishna|4|*",            "Sankashti Chaturthi",          "संकष्टी चतुर्थी"),
        Triple("Shukla|4|*",             "Vinayaka Chaturthi",           "विनायक चतुर्थी"),
        Triple("Shukla|11|*",            "Ekadashi Vrat",                "एकादशी व्रत"),
        Triple("Krishna|11|*",           "Ekadashi Vrat",                "एकादशी व्रत")
    )
    fun festivalsFor(p: Panchang): List<Festival> {
        val wpTithi = (p.tithiNum % 15) + 1
        val out = mutableListOf<Festival>()
        for ((key, en, hi) in FESTIVALS) {
            val parts = key.split("|")
            if (parts[0] != p.paksha) continue
            if (parts[1].toInt() != wpTithi) continue
            if (parts[2] != "*" && parts[2] != p.hinduMonth) continue
            out.add(Festival(en, hi))
        }
        return out
    }

    /** Search forward up to `maxDays` days from `from` for the next festival. */
    fun nextFestival(from: Calendar, lat: Double, lon: Double, maxDays: Int = 60): Pair<Calendar, Festival>? {
        val cal = from.clone() as Calendar
        for (i in 0 until maxDays) {
            val p = computeFor(cal, lat, lon)
            val f = festivalsFor(p).firstOrNull()
            if (f != null) return Pair(cal.clone() as Calendar, f)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return null
    }

    // ---------- Dietary lookup ----------
    data class Dietary(val tithiAvoid: String, val tithiReason: String, val vaarGraha: String, val vaarAvoid: String,
                       val maasAvoid: String, val maasPrefer: String, val maasApprox: String, val nakTree: String)

    private val TITHI_AVOID = arrayOf(
        Pair("Kaddu / petha (pumpkin, ash gourd)",                  "Loss of wealth"),
        Pair("Baingan (brinjal)",                                    ""),
        Pair("Parwal (pointed gourd)",                               "Increase in enemies"),
        Pair("Mooli (radish)",                                       "Loss of wealth"),
        Pair("Bel / bilva fruit",                                    "Brings disrepute"),
        Pair("Neem (leaves, fruit, even datun)",                     "Leads to a lower birth"),
        Pair("Taad fruit (palm)",                                    "Increases disease"),
        Pair("Nariyal (coconut)",                                    "Destroys intellect"),
        Pair("Lauki (bottle gourd)",                                 "Equal to eating beef"),
        Pair("Kalambi (water spinach)",                              ""),
        Pair("Sem ki phali (beans)",                                 ""),
        Pair("Poi / putika (Malabar spinach)",                       ""),
        Pair("Baingan (brinjal)",                                    ""),
        Pair("Dahi (curd)",                                          ""),
        Pair("Dahi (curd)",                                          "")
    )
    private val VAAR_AVOID = arrayOf(
        Pair("Surya",   "Masoor dal, ginger, red vegetables"),
        Pair("Chandra", "Sugar (chini)"),
        Pair("Mangal",  "Ghee, dark foods, non-veg"),
        Pair("Budh",    "Green vegetables"),
        Pair("Guru",    "Milk, bananas, non-veg"),
        Pair("Shukra",  "Sour things (khatai)"),
        Pair("Shani",   "Alcohol, non-veg")
    )
    private val MAAS_GUIDE = mapOf(
        "Chaitra"      to Triple("Mar–Apr", "Gud (jaggery)",                         "Chana"),
        "Vaishakha"    to Triple("Apr–May", "Tel (oil)",                             "Bel"),
        "Jyeshtha"     to Triple("May–Jun", "Long travel / exertion",                "Afternoon rest"),
        "Ashadha"      to Triple("Jun–Jul", "Bel",                                   "Physical activity"),
        "Shravana"     to Triple("Jul–Aug", "Saag (leafy greens)",                   "Harre (haritaki)"),
        "Bhadrapada"   to Triple("Aug–Sep", "Mahi / dahi (buttermilk, curd)",        "Til (sesame)"),
        "Ashwina"      to Triple("Sep–Oct", "Karela (bitter gourd)",                 "Gud, daily"),
        "Kartika"      to Triple("Oct–Nov", "Dahi (curd)",                           "Mool (radish)"),
        "Margashirsha" to Triple("Nov–Dec", "Jeera (cumin)",                         "Tel (oil)"),
        "Pausha"       to Triple("Dec–Jan", "Dhaniya (coriander) — cooling",         "Doodh (milk)"),
        "Magha"        to Triple("Jan–Feb", "Mishri (rock sugar)",                   "Ghee-khichdi"),
        "Phalguna"     to Triple("Feb–Mar", "Chana (gram)",                          "Wake early, bathe daily")
    )
    private val NAK_TREE = arrayOf(
        "Kuchla","Amla","Gular","Jamun","Khair","Agar","Bamboo","Peepal","Nagkesar",
        "Bargad","Palash","Pakar","Reetha","Bel","Arjun","Kaith","Maulsari","Semal",
        "Saal","Jalvetas","Kathal","Aak","Shami","Kadamb","Aam","Neem","Mahua"
    )

    fun dietaryFor(p: Panchang): Dietary {
        val wp = (p.tithiNum % 15) + 1
        val t = TITHI_AVOID[wp - 1]
        val v = VAAR_AVOID[p.weekday]
        val m = MAAS_GUIDE[p.hinduMonth] ?: Triple("","","")
        val tree = NAK_TREE[p.nakshatra.idx]
        return Dietary(t.first, t.second, v.first, v.second, m.second, m.third, m.first, tree)
    }
}

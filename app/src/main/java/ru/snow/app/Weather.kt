package ru.snow.app

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

enum class SnowStatus(val title: String) {
    HEAVY("Сильный снегопад"),
    SNOW("Снег идёт"),
    LIGHT("Слабый снег"),
    EXPECTED("Снег ожидается"),
    NONE("Снега нет"),
}

data class Place(val name: String, val lat: Double, val lon: Double, val sub: String = "")

data class Hour(val time: Long, val temp: Double?, val code: Int?, val snowfall: Double?) {
    val snowy: Boolean get() = isSnowCode(code) || (snowfall ?: 0.0) > 0.0
}

data class Weather(
    val city: String,
    val updatedAt: Long,
    val tz: String,
    val temp: Double?,
    val feels: Double?,
    val humidity: Double?,
    val wind: Double?,
    val code: Int?,
    val snowfall: Double?,
    val now: Long,
    val hours: List<Hour>,
    val ahead: List<Hour>,
    val history: List<Hour>,
)

data class DaySummary(val label: String, val min: Double?, val max: Double?, val snow: String, val periods: String?)

private val SNOW_CODES = setOf(71, 73, 75, 77, 85, 86)

fun isSnowCode(code: Int?): Boolean = code != null && code in SNOW_CODES

private fun JSONObject.num(k: String): Double? = if (has(k) && !isNull(k)) optDouble(k) else null

private fun JSONArray.numAt(i: Int): Double? = if (i < length() && !isNull(i)) optDouble(i) else null

fun parseWeather(raw: JSONObject, city: String, updatedAt: Long): Weather {
    val cur = raw.optJSONObject("current")
    val hr = raw.optJSONObject("hourly")
    val now = cur?.num("time")?.toLong() ?: (System.currentTimeMillis() / 1000)
    val times = hr?.optJSONArray("time")
    val temps = hr?.optJSONArray("temperature_2m")
    val codes = hr?.optJSONArray("weather_code")
    val snows = hr?.optJSONArray("snowfall")
    val all = ArrayList<Hour>()
    if (times != null) {
        for (i in 0 until times.length()) {
            all.add(Hour(times.optLong(i), temps?.numAt(i), codes?.numAt(i)?.toInt(), snows?.numAt(i)))
        }
    }
    val future = all.filter { it.time >= now - 3599 }
    val history = all.filter { it.time <= now }
    return Weather(
        city = city,
        updatedAt = updatedAt,
        tz = raw.optString("timezone", "UTC"),
        temp = cur?.num("temperature_2m"),
        feels = cur?.num("apparent_temperature"),
        humidity = cur?.num("relative_humidity_2m"),
        wind = cur?.num("wind_speed_10m"),
        code = cur?.num("weather_code")?.toInt(),
        snowfall = cur?.num("snowfall"),
        now = now,
        hours = future.take(24),
        ahead = future,
        history = history,
    )
}

private fun Weather.dayKey(sec: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone(tz) }.format(Date(sec * 1000))

private fun Weather.summarize(label: String, hs: List<Hour>): DaySummary {
    val periods = ArrayList<String>()
    var start = -1L
    var last = -1L
    for (h in hs) {
        if (h.snowy) {
            if (start < 0) start = h.time
            last = h.time
        } else if (start >= 0) {
            periods.add("${fmtHour(start)}–${fmtHour(last + 3600)}")
            start = -1
        }
    }
    if (start >= 0) periods.add("${fmtHour(start)}–${fmtHour(last + 3600)}")
    val cm = hs.sumOf { it.snowfall ?: 0.0 }
    val snow = when {
        cm >= 0.05 -> String.format(Locale("ru"), "Снег %.1f см", cm)
        periods.isNotEmpty() -> "Снег"
        else -> "Без снега"
    }
    return DaySummary(
        label = label,
        min = hs.mapNotNull { it.temp }.minOrNull(),
        max = hs.mapNotNull { it.temp }.maxOrNull(),
        snow = snow,
        periods = periods.take(3).joinToString(", ").ifEmpty { null },
    )
}

private fun Weather.dayName(sec: Long): String {
    val f = SimpleDateFormat("EEEE, d MMMM", Locale("ru")).apply { timeZone = TimeZone.getTimeZone(tz) }
    return f.format(Date(sec * 1000)).cap()
}

/** История по дням (сегодня первым). Данные модели Open-Meteo за последние 7 суток. */
fun Weather.days(): List<DaySummary> {
    val today = dayKey(now)
    val yesterday = dayKey(now - 86400)
    return history.sortedBy { it.time }.groupBy { dayKey(it.time) }.entries.sortedByDescending { it.key }.map { (d, hs) ->
        summarize(
            when (d) {
                today -> "Сегодня"
                yesterday -> "Вчера"
                else -> dayName(hs.first().time)
            },
            hs,
        )
    }
}

/** Прогноз по дням вперёд (сегодня первым). */
fun Weather.forecastDays(): List<DaySummary> {
    val today = dayKey(now)
    val tomorrow = dayKey(now + 86400)
    return ahead.sortedBy { it.time }.groupBy { dayKey(it.time) }.entries.sortedBy { it.key }.map { (d, hs) ->
        summarize(
            when (d) {
                today -> "Сегодня"
                tomorrow -> "Завтра"
                else -> dayName(hs.first().time)
            },
            hs,
        )
    }
}

fun String.cap(): String = replaceFirstChar { it.uppercase() }

private fun Weather.weekdayIn(sec: Long): String {
    val c = java.util.Calendar.getInstance(TimeZone.getTimeZone(tz))
    c.timeInMillis = sec * 1000
    return arrayOf("в воскресенье", "в понедельник", "во вторник", "в среду", "в четверг", "в пятницу", "в субботу")[c.get(java.util.Calendar.DAY_OF_WEEK) - 1]
}

private fun Weather.whenText(sec: Long): String {
    val k = dayKey(sec)
    val time = fmtHour(sec)
    return when (k) {
        dayKey(now) -> "сегодня около $time"
        dayKey(now + 86400) -> "завтра около $time"
        else -> "${weekdayIn(sec)} около $time"
    }
}

private fun Weather.horizonText(): String {
    val last = ahead.lastOrNull()?.time ?: return ""
    val h = ((last - now) / 3600).toInt()
    return if (h >= 150) "7 дней" else "$h ч"
}

/** Начало ближайшего снега: если снег идёт сейчас — следующего после паузы. */
fun Weather.nextSnow(): Hour? {
    val fut = ahead.filter { it.time > now }
    if (status().snowing()) {
        val end = fut.indexOfFirst { !it.snowy }
        if (end < 0) return null
        return fut.drop(end).firstOrNull { it.snowy }
    }
    return fut.firstOrNull { it.snowy }
}

fun Weather.nextSnowTitle(): String = if (status().snowing()) "Следующий снег" else "Ближайший снег"

/** Текст со строчной буквы; для отображения — .cap(). */
fun Weather.nextSnowBody(): String {
    val fut = ahead.filter { it.time > now }
    if (fut.isEmpty()) return "нет данных"
    val n = nextSnow()
    if (n != null) return whenText(n.time)
    return when {
        status().snowing() && fut.all { it.snowy } -> "снег не прекратится в ближайшие ${horizonText()}"
        status().snowing() -> "после этого снега в ближайшие ${horizonText()} не ожидается"
        else -> "снега в ближайшие ${horizonText()} не ожидается"
    }
}

fun Weather.nextSnowAmount(): String? {
    val n = nextSnow() ?: return null
    var cm = 0.0
    for (h in ahead.filter { it.time >= n.time }) {
        if (!h.snowy) break
        cm += h.snowfall ?: 0.0
    }
    return if (cm >= 0.1) String.format(Locale("ru"), "Около %.1f см", cm) else null
}

/** Статус снега по данным: код погоды и количество осадков, а не только температура. Null — нет данных. */
fun Weather.status(): SnowStatus? {
    if (code == null && temp == null && snowfall == null && hours.isEmpty()) return null
    val sf = snowfall ?: 0.0
    if (isSnowCode(code) || sf > 0.0) {
        val byCode = when (code) {
            75, 86 -> 3
            73 -> 2
            71, 77, 85 -> 1
            else -> 0
        }
        val bySnowfall = when {
            sf >= 1.5 -> 3
            sf >= 0.3 -> 2
            sf > 0.0 -> 1
            else -> 0
        }
        return when (maxOf(byCode, bySnowfall)) {
            3 -> SnowStatus.HEAVY
            2 -> SnowStatus.SNOW
            else -> SnowStatus.LIGHT
        }
    }
    val soon = hours.filter { it.time > now }.take(6).any { it.snowy }
    return if (soon) SnowStatus.EXPECTED else SnowStatus.NONE
}

fun SnowStatus?.level(): Int = when (this) {
    SnowStatus.HEAVY -> 3
    SnowStatus.SNOW -> 2
    SnowStatus.LIGHT -> 1
    else -> 0
}

fun SnowStatus?.snowing(): Boolean = level() > 0

fun Weather.fmtHour(sec: Long): String {
    val f = SimpleDateFormat("HH:mm", Locale("ru"))
    f.timeZone = TimeZone.getTimeZone(tz)
    return f.format(Date(sec * 1000))
}

fun Weather.hourLabel(h: Hour, index: Int): String = if (index == 0 && h.time <= now) "Сейчас" else fmtHour(h.time)

fun Weather.detail(): String? {
    val st = status() ?: return null
    return when (st) {
        SnowStatus.EXPECTED -> hours.firstOrNull { it.time > now && it.snowy }?.let { "Начнётся ${whenText(it.time)}" }
        SnowStatus.NONE -> null
        else -> {
            val end = ahead.firstOrNull { it.time > now && !it.snowy }
            when {
                end != null -> "Закончится около ${fmtHour(end.time)}"
                ahead.any { it.time > now } -> "Снег продолжится"
                else -> null
            }
        }
    }
}

fun fmtTemp(v: Double?): String? {
    if (v == null) return null
    val r = v.roundToInt()
    return if (r < 0) "−${-r}°" else "$r°"
}

fun fmtWind(v: Double?): String = if (v == null) "Нет данных" else "${v.roundToInt()} м/с"

fun fmtHumidity(v: Double?): String = if (v == null) "Нет данных" else "${v.roundToInt()}%"

fun fmtLocalTime(millis: Long): String = SimpleDateFormat("HH:mm", Locale("ru")).format(Date(millis))

fun iconRes(code: Int?, snowy: Boolean, flake: Boolean = false): Int = when {
    snowy || isSnowCode(code) -> if (flake) R.drawable.ic_snow else R.drawable.ic_cloud_snow
    code == null -> R.drawable.ic_cloud
    code == 0 -> R.drawable.ic_clear
    code == 1 || code == 2 -> R.drawable.ic_partly
    code == 3 -> R.drawable.ic_cloud
    code in 45..48 -> R.drawable.ic_fog
    code in 51..67 || code in 80..82 -> R.drawable.ic_rain
    code >= 95 -> R.drawable.ic_storm
    else -> R.drawable.ic_cloud
}

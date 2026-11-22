package ru.snow.app

import android.content.Context
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/** Когда снег обычно приходит в этом месте — по фактическим данным архива Open-Meteo за прошлые годы. */
object Climo {
    private const val YEARS = 10

    /** Скачивает дни со снегом за 10 лет (раз в 2 недели и при смене места). */
    fun ensure(c: Context, p: Place) {
        val cur = Prefs.climo(c)
        if (cur != null && abs(cur.lat - p.lat) < 0.05 && abs(cur.lon - p.lon) < 0.05 &&
            System.currentTimeMillis() - cur.at < 14 * 86_400_000L
        ) return
        val end = LocalDate.now().minusDays(8)
        val start = end.minusYears(YEARS.toLong())
        val j = Api.archive(p.lat, p.lon, start.toString(), end.toString())
        val d = j.optJSONObject("daily") ?: return
        val t = d.optJSONArray("time") ?: return
        val s = d.optJSONArray("snowfall_sum") ?: return
        var known = 0
        val days = ArrayList<Int>()
        for (i in 0 until t.length()) {
            if (s.isNull(i)) continue
            known++
            if (s.optDouble(i) >= 0.1) days.add(LocalDate.parse(t.getString(i)).toEpochDay().toInt())
        }
        if (known < 365) return
        Prefs.saveClimo(c, p, end.toEpochDay().toInt(), days)
    }

    /** Типичная дата первого снега после anchor и число лет, по которым посчитано. Нужно минимум 5 лет. */
    private fun typical(c: Context, anchor: LocalDate): Pair<LocalDate, Int>? {
        val cl = Prefs.climo(c) ?: return null
        val days = cl.days.sorted()
        val offsets = ArrayList<Int>()
        for (k in 1..YEARS) {
            val a = anchor.minusYears(k.toLong()).toEpochDay().toInt()
            if (a > cl.end) continue
            val f = days.firstOrNull { it >= a } ?: continue
            if (f - a > 365) continue
            offsets.add(f - a)
        }
        if (offsets.size < 5) return null
        offsets.sort()
        return anchor.plusDays(offsets[offsets.size / 2].toLong()) to offsets.size
    }

    fun withTypical(c: Context, w: Weather): Weather = try {
        val last = w.ahead.lastOrNull()?.time
        val anchor = if (last != null) Instant.ofEpochSecond(last).atZone(ZoneId.of(w.tz)).toLocalDate() else LocalDate.now()
        val t = typical(c, anchor)
        if (t == null) {
            w
        } else {
            val f = SimpleDateFormat("d MMMM", Locale("ru")).apply { timeZone = TimeZone.getTimeZone("UTC") }
            w.copy(typicalDate = f.format(Date(t.first.toEpochDay() * 86_400_000L)), typicalYears = t.second)
        }
    } catch (e: Exception) {
        w
    }
}

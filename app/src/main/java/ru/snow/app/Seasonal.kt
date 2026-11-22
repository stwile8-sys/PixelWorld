package ru.snow.app

import android.content.Context
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import kotlin.math.min

/**
 * Сезонный прогноз Open-Meteo (ECMWF, 51 сценарий) на 100 дней вперёд.
 * Точность низкая: это оценка, когда снег вероятен, а не прогноз на конкретный день.
 */
object Seasonal {
    private const val HORIZON = 100

    fun ensure(c: Context, p: Place) {
        val cur = Prefs.seasonal(c)
        if (cur != null && kotlin.math.abs(cur.lat - p.lat) < 0.05 && kotlin.math.abs(cur.lon - p.lon) < 0.05 &&
            System.currentTimeMillis() - cur.at < 12 * 3_600_000L
        ) return
        val j = try {
            Api.seasonal(p.lat, p.lon, "snowfall_sum,precipitation_sum,temperature_2m_mean")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Api.seasonal(p.lat, p.lon, "snowfall_sum")
        }
        val d = j.optJSONObject("daily") ?: return
        val times = d.optJSONArray("time") ?: return
        val dates = IntArray(times.length()) { LocalDate.parse(times.getString(it)).toEpochDay().toInt() }
        val keys = d.keys().asSequence().toList()
        fun group(v: String) = keys.filter { it == v || it.startsWith(v + "_") }.associateBy { it.removePrefix(v) }
        val sn = group("snowfall_sum")
        val pr = group("precipitation_sum")
        val tm = group("temperature_2m_mean")
        val suffixes = (sn.keys + pr.keys).toSet().let { if (it.size > 1) it - "" else it }
        val members = ArrayList<IntArray>()
        var end = 0
        for (sf in suffixes) {
            val a = sn[sf]?.let { d.optJSONArray(it) }
            val pa = pr[sf]?.let { d.optJSONArray(it) }
            val ta = tm[sf]?.let { d.optJSONArray(it) }
            val snowy = ArrayList<Int>()
            var known = 0
            var last = 0
            for (i in dates.indices) {
                val s = if (a != null && !a.isNull(i)) a.optDouble(i) else null
                val isSnow: Boolean? = when {
                    s != null -> s >= 0.1
                    pa != null && ta != null && !pa.isNull(i) && !ta.isNull(i) -> pa.optDouble(i) >= 0.3 && ta.optDouble(i) <= 1.0
                    else -> null
                }
                if (isSnow != null) {
                    known++
                    last = dates[i]
                }
                if (isSnow == true) snowy.add(dates[i])
            }
            if (known >= 30) {
                members.add(snowy.toIntArray())
                end = maxOf(end, last)
            }
        }
        if (members.size < 10) return
        Prefs.saveSeasonal(c, p, end, members)
    }

    fun withSeasonal(c: Context, w: Weather): Weather = try {
        val cl = Prefs.seasonal(c)
        val zone = ZoneId.of(w.tz)
        val today = Instant.ofEpochSecond(w.now).atZone(zone).toLocalDate().toEpochDay().toInt()
        val last = w.ahead.lastOrNull()?.time
        val anchor = if (last != null) Instant.ofEpochSecond(last).atZone(zone).toLocalDate().toEpochDay().toInt() else today
        if (cl == null) {
            w
        } else {
            val lastDay = min(cl.end, today + HORIZON)
            if (lastDay - anchor < 14) {
                w
            } else {
                val firsts = cl.members.map { m -> m.firstOrNull { it > anchor && it <= lastDay } }
                val total = firsts.size
                val withSnow = firsts.filterNotNull().sorted()
                val mid = total / 2
                if (withSnow.size <= mid) {
                    w.copy(seasonalNoSnow = true, seasonalDays = lastDay - today)
                } else {
                    val q1 = withSnow.getOrNull(total / 4)
                    val q3 = withSnow.getOrNull(total * 3 / 4)
                    val range = if (q1 != null && q3 != null && q1 != q3) "между ${Climo.fmtDay(q1)} и ${Climo.fmtDay(q3)}" else null
                    w.copy(seasonalDate = Climo.fmtDay(withSnow[mid]), seasonalRange = range, seasonalDays = lastDay - today)
                }
            }
        }
    } catch (e: Exception) {
        w
    }
}

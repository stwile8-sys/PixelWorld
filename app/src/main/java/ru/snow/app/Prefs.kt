package ru.snow.app

import android.content.Context
import org.json.JSONObject
import kotlin.math.abs

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("snow", Context.MODE_PRIVATE)

    fun theme(c: Context): String = sp(c).getString("theme", "system") ?: "system"
    fun setTheme(c: Context, v: String) = sp(c).edit().putString("theme", v).apply()

    fun anim(c: Context): Boolean = sp(c).getBoolean("anim", true)
    fun setAnim(c: Context, v: Boolean) = sp(c).edit().putBoolean("anim", v).apply()

    fun auto(c: Context): Boolean = sp(c).getBoolean("auto", true)
    fun setAuto(c: Context, v: Boolean) = sp(c).edit().putBoolean("auto", v).apply()

    fun place(c: Context): Place? {
        val s = sp(c)
        val name = s.getString("place_name", null) ?: return null
        val lat = s.getString("place_lat", null)?.toDoubleOrNull() ?: return null
        val lon = s.getString("place_lon", null)?.toDoubleOrNull() ?: return null
        return Place(name, lat, lon)
    }

    fun setPlace(c: Context, p: Place, auto: Boolean) {
        val old = place(c)
        val e = sp(c).edit()
            .putString("place_name", p.name)
            .putString("place_lat", p.lat.toString())
            .putString("place_lon", p.lon.toString())
            .putBoolean("auto", auto)
        // Сохранённая погода относится к старому месту — сбрасываем, если место заметно сменилось.
        if (old == null || abs(old.lat - p.lat) > 0.05 || abs(old.lon - p.lon) > 0.05) e.remove("snap")
        e.apply()
    }

    fun snapshot(c: Context): Weather? {
        val s = sp(c).getString("snap", null) ?: return null
        return try {
            val j = JSONObject(s)
            parseWeather(j.getJSONObject("raw"), j.getString("city"), j.getLong("at"))
        } catch (e: Exception) {
            null
        }
    }

    fun saveSnapshot(c: Context, raw: JSONObject, city: String, at: Long) {
        val j = JSONObject().put("raw", raw).put("city", city).put("at", at)
        sp(c).edit().putString("snap", j.toString()).apply()
    }
}

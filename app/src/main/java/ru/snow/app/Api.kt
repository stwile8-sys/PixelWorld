package ru.snow.app

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object Api {
    private fun get(url: String): JSONObject {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 15000
        try {
            if (c.responseCode != 200) throw IOException("HTTP ${c.responseCode}")
            return JSONObject(c.inputStream.bufferedReader().use { it.readText() })
        } finally {
            c.disconnect()
        }
    }

    fun forecast(lat: Double, lon: Double): JSONObject = get(
        "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,weather_code,snowfall" +
            "&hourly=temperature_2m,weather_code,snowfall&forecast_days=16&past_days=7" +
            "&wind_speed_unit=ms&timeformat=unixtime&timezone=auto"
    )

    fun archive(lat: Double, lon: Double, start: String, end: String): JSONObject = get(
        "https://archive-api.open-meteo.com/v1/archive?latitude=$lat&longitude=$lon" +
            "&start_date=$start&end_date=$end&daily=snowfall_sum&timezone=auto"
    )

    fun search(q: String): List<Place> {
        val j = get(
            "https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(q, "UTF-8")}" +
                "&count=8&language=ru&format=json"
        )
        val a = j.optJSONArray("results") ?: return emptyList()
        val out = ArrayList<Place>()
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            val sub = listOf(o.optString("admin1"), o.optString("country"))
                .filter { it.isNotBlank() }
                .joinToString(", ")
            out.add(Place(o.getString("name"), o.getDouble("latitude"), o.getDouble("longitude"), sub))
        }
        return out
    }
}

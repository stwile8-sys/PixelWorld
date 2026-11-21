package ru.snow.app

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

object Loc {
    @SuppressLint("MissingPermission")
    suspend fun locate(c: Context): Location? {
        val lm = c.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = try {
            lm.getProviders(true).filter { it != LocationManager.PASSIVE_PROVIDER }
        } catch (e: Exception) {
            emptyList()
        }
        var best: Location? = null
        for (p in providers) {
            val l = try {
                lm.getLastKnownLocation(p)
            } catch (e: Exception) {
                null
            }
            if (l != null && l.time > (best?.time ?: 0L)) best = l
        }
        val known = best
        if (known != null && System.currentTimeMillis() - known.time < 15 * 60_000L) return known
        if (Build.VERSION.SDK_INT >= 30 && providers.isNotEmpty()) {
            val prov = if (LocationManager.NETWORK_PROVIDER in providers) LocationManager.NETWORK_PROVIDER else providers.first()
            val fresh = withTimeoutOrNull(10_000L) {
                suspendCancellableCoroutine<Location?> { cont ->
                    val cs = CancellationSignal()
                    cont.invokeOnCancellation { cs.cancel() }
                    try {
                        lm.getCurrentLocation(prov, cs, c.mainExecutor) { loc -> cont.resume(loc) }
                    } catch (e: Exception) {
                        cont.resume(null)
                    }
                }
            }
            if (fresh != null) return fresh
        }
        return known
    }

    @Suppress("DEPRECATION")
    suspend fun cityName(c: Context, lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) return@withContext null
            val a = Geocoder(c, Locale("ru")).getFromLocation(lat, lon, 1)
            val x = a?.firstOrNull() ?: return@withContext null
            x.locality ?: x.subAdminArea ?: x.adminArea
        } catch (e: Exception) {
            null
        }
    }
}

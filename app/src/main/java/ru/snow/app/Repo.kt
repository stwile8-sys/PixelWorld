package ru.snow.app

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Repo {
    /** Загружает погоду для сохранённого места. true — успех; при ошибке остаются последние сохранённые данные. */
    suspend fun refresh(c: Context): Boolean = withContext(Dispatchers.IO) {
        val p = Prefs.place(c) ?: return@withContext false
        try {
            val raw = Api.forecast(p.lat, p.lon)
            val now = Prefs.place(c)
            if (now == null || now.lat != p.lat || now.lon != p.lon) return@withContext false
            Prefs.saveSnapshot(c, raw, now.name, System.currentTimeMillis())
            try {
                Seasonal.ensure(c, now)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // сезонный прогноз необязателен
            }
            try {
                Climo.ensure(c, now)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // статистика прошлых лет необязательна
            }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateWidgets(c: Context) {
        try {
            SmallWidget().updateAll(c)
            MediumWidget().updateAll(c)
            LargeWidget().updateAll(c)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // виджеты не критичны для работы приложения
        }
    }
}

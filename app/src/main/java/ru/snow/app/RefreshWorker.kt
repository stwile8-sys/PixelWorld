package ru.snow.app

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Фоновое обновление для виджетов: раз в 30 минут, только при сети и не при низком заряде. */
class RefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val c = applicationContext
        if (widgetCount(c) == 0) {
            WorkManager.getInstance(c).cancelUniqueWork(NAME)
            return Result.success()
        }
        if (Prefs.place(c) == null) return Result.success()
        val ok = Repo.refresh(c)
        Repo.updateWidgets(c)
        return if (ok) Result.success() else Result.retry()
    }

    companion object {
        private const val NAME = "snow_refresh"
        private const val NOW = "snow_refresh_now"

        private fun widgetCount(c: Context): Int {
            val m = AppWidgetManager.getInstance(c)
            return listOf(SmallReceiver::class.java, MediumReceiver::class.java, LargeReceiver::class.java)
                .sumOf { m.getAppWidgetIds(ComponentName(c, it)).size }
        }

        private fun constraints(battery: Boolean) = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(battery)
            .build()

        /** Раз в 15 минут — минимальный период фоновой работы в Android. */
        fun schedule(c: Context, force: Boolean = false) {
            if (!force && widgetCount(c) == 0) return
            val req = PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints(false))
                .build()
            WorkManager.getInstance(c).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
        }

        fun runNow(c: Context) {
            val req = OneTimeWorkRequestBuilder<RefreshWorker>()
                .setConstraints(constraints(false))
                .build()
            WorkManager.getInstance(c).enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, req)
        }
    }
}

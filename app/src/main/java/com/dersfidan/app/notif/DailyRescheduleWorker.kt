package com.dersfidan.app.notif

import android.content.Context
import androidx.work.*
import com.dersfidan.app.data.Repository
import java.util.concurrent.TimeUnit

class DailyRescheduleWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repo = Repository(applicationContext)
        NotificationScheduler.planlaBugunVeYarin(applicationContext, repo)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "gunluk_alarm_yenileme"

        fun planla(context: Context) {
            val istek = PeriodicWorkRequestBuilder<DailyRescheduleWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(1, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, istek
            )
        }
    }
}

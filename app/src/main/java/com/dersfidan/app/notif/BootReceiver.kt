package com.dersfidan.app.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val req = OneTimeWorkRequestBuilder<DailyRescheduleWorker>().build()
            WorkManager.getInstance(context).enqueue(req)
        }
    }
}

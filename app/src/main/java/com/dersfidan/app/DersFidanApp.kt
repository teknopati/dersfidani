package com.dersfidan.app

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.dersfidan.app.audio.SesYoneticisi
import com.dersfidan.app.data.Repository
import com.dersfidan.app.notif.DailyRescheduleWorker
import com.dersfidan.app.notif.NotificationScheduler
import kotlinx.coroutines.launch

class DersFidanApp : Application() {

    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        SesYoneticisi.baslat(this)
        repository = Repository(this)

        ProcessLifecycleOwner.get().lifecycleScope.launch {
            repository.ilkYuklemeYapGerekirse()
            NotificationScheduler.planlaBugunVeYarin(this@DersFidanApp, repository)
            DailyRescheduleWorker.planla(this@DersFidanApp)
        }
    }
}

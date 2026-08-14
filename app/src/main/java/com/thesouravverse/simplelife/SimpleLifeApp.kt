package com.thesouravverse.simplelife

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.thesouravverse.simplelife.work.ExportWorker
import com.thesouravverse.simplelife.work.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SimpleLifeApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var workScheduler: WorkScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createExportChannel()
        workScheduler.scheduleDailyPenalty()
        workScheduler.scheduleDailyExport()
        workScheduler.scheduleSync()
        workScheduler.syncNow()
    }

    private fun createExportChannel() {
        val channel = NotificationChannel(
            ExportWorker.CHANNEL_ID,
            "Daily export",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

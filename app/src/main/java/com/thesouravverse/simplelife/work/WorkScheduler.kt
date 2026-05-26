package com.thesouravverse.simplelife.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    fun scheduleDailyPenalty() {
        val now = LocalDateTime.now()
        // Run shortly after midnight so we roll over yesterday's incompletes
        // and the home-screen widget refreshes for the new day.
        val nextRun = LocalDateTime.of(LocalDate.now(), LocalTime.of(0, 5))
            .let { if (it.isBefore(now)) it.plusDays(1) else it }
        val initialDelay = Duration.between(now, nextRun).toMillis()
            .coerceAtLeast(0L)

        val request = PeriodicWorkRequestBuilder<PenaltyWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    companion object {
        private const val UNIQUE_NAME = "SimpleLife-daily-penalty"
    }
}

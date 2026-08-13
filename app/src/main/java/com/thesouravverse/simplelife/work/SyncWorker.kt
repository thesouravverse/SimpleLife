package com.thesouravverse.simplelife.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.thesouravverse.simplelife.sync.SyncRepository
import com.thesouravverse.simplelife.sync.SyncSettings
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val syncRepo: SyncRepository,
    private val settings: SyncSettings
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        if (!settings.isConfigured()) return Result.success()
        return runCatching {
            val inserted = syncRepo.pullInbox()
            syncRepo.pushStatus()
            val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            settings.setLastResult("Synced $inserted new task${if (inserted == 1) "" else "s"} \u00b7 $time")
            Result.success()
        }.getOrElse { e ->
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                settings.setLastResult("Sync failed \u00b7 $time \u00b7 ${e.message.orEmpty().take(120)}")
                Result.success()
            }
        }
    }
}

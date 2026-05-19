package com.thesouravverse.dayquest.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.thesouravverse.dayquest.data.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

@HiltWorker
class PenaltyWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val repo: TaskRepository
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = runCatching {
        repo.applyMissedPenalties(LocalDate.now())
        Result.success()
    }.getOrElse { Result.retry() }
}

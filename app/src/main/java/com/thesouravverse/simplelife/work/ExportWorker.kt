package com.thesouravverse.simplelife.work

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.thesouravverse.simplelife.R
import com.thesouravverse.simplelife.data.TaskRepository
import com.thesouravverse.simplelife.data.db.TaskDao
import com.thesouravverse.simplelife.sync.TaskExporter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.io.File
import java.time.LocalDate

@HiltWorker
class ExportWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val exporter: TaskExporter,
    private val taskDao: TaskDao,
    private val taskRepo: TaskRepository
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = runCatching {
        // Persist to shared storage for third-party sync apps (best effort).
        runCatching { exporter.writeToSharedStorage() }
        // Cache copy backs the share sheet.
        val file = exporter.writeToCache()
        postReadyNotification(file)
        Result.success()
    }.getOrElse { Result.success() } // never break the periodic chain

    private suspend fun postReadyNotification(file: File) {
        val ctx = applicationContext
        // On API 33+ we can't post without the runtime permission.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val todayEpoch = LocalDate.now().toEpochDay()
        val all = taskDao.allTasksFlow().first()
        val todayParents = all.filter { it.parentId == null && it.dayEpochDay == todayEpoch }
        val done = todayParents.count { it.completed }
        val total = todayParents.size
        val xp = taskRepo.totalXpFlow().first()

        val sharePendingIntent = PendingIntent.getActivity(
            ctx,
            0,
            exporter.buildShareIntent(file),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("Today's export is ready")
            .setContentText("$done of $total done \u00b7 +$xp XP")
            .setContentIntent(sharePendingIntent)
            .addAction(0, "Share", sharePendingIntent)
            .setAutoCancel(true)
            .build()

        runCatching { NotificationManagerCompat.from(ctx).notify(NOTIF_ID, notification) }
    }

    companion object {
        const val CHANNEL_ID = "exports"
        private const val NOTIF_ID = 4201
    }
}

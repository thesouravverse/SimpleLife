package com.thesouravverse.simplelife.sync

import android.content.ClipData
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.thesouravverse.simplelife.data.TaskRepository
import com.thesouravverse.simplelife.data.db.SyncedTaskDao
import com.thesouravverse.simplelife.data.db.TaskDao
import com.thesouravverse.simplelife.data.db.TaskEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskExporter @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val syncedDao: SyncedTaskDao,
    private val taskDao: TaskDao,
    private val taskRepo: TaskRepository
) {
    /** Builds the report JSON string covering the last [daysBack] days. */
    suspend fun buildJson(daysBack: Long = 14): String {
        val all = taskDao.allTasksFlow().first()
        val totalXp = taskRepo.totalXpFlow().first()
        val uidByLocalId = syncedDao.all().associate { it.localId to it.uid }
        val cutoff = LocalDate.now().minusDays(daysBack).toEpochDay()
        val subsByParent = all.filter { it.parentId != null }.groupBy { it.parentId!! }

        val days = all
            .filter { it.parentId == null && it.dayEpochDay >= cutoff }
            .groupBy { it.dayEpochDay }
            .toSortedMap()
            .map { (epochDay, parents) ->
                DayStatus(
                    date = LocalDate.ofEpochDay(epochDay).toString(),
                    tasks = parents.map { p ->
                        p.toStatus(uidByLocalId[p.id]).copy(
                            subtasks = subsByParent[p.id].orEmpty().map { it.toStatus(null) }
                        )
                    }
                )
            }

        val report = StatusReport(
            updatedAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()),
            totalXp = totalXp,
            days = days
        )
        return SyncJson.encodeToString(report)
    }

    /** Writes the export to cacheDir/exports/simplelife_<today>.json for the share sheet. */
    suspend fun writeToCache(daysBack: Long = 14): File = withContext(Dispatchers.IO) {
        val json = buildJson(daysBack)
        val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName())
        file.writeText(json)
        file
    }

    /**
     * Writes the export to shared storage so third-party sync apps can read it:
     * Documents/SimpleLife/simplelife_<today>.json, overwriting a same-named file.
     * No storage permission needed on API 29+; legacy path uses WRITE_EXTERNAL_STORAGE.
     * Returns the written Uri.
     */
    suspend fun writeToSharedStorage(daysBack: Long = 14): Uri = withContext(Dispatchers.IO) {
        val json = buildJson(daysBack)
        val name = fileName()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = ctx.contentResolver
            val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val relPath = Environment.DIRECTORY_DOCUMENTS + "/SimpleLife/"

            // Overwrite: delete any existing file with the same name in that folder.
            val selection =
                "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?"
            resolver.query(
                collection,
                arrayOf(MediaStore.MediaColumns._ID),
                selection,
                arrayOf(relPath, name),
                null
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                while (c.moveToNext()) {
                    resolver.delete(ContentUris.withAppendedId(collection, c.getLong(idCol)), null, null)
                }
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
            }
            val uri = resolver.insert(collection, values)
                ?: throw IOException("MediaStore insert failed")
            resolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                ?: throw IOException("Couldn't open output stream")
            uri
        } else {
            // Legacy API 26–28: public Documents/SimpleLife via WRITE_EXTERNAL_STORAGE.
            @Suppress("DEPRECATION")
            val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val dir = File(docs, "SimpleLife").apply { mkdirs() }
            val file = File(dir, name)
            file.writeText(json)
            Uri.fromFile(file)
        }
    }

    /** Builds a share-sheet chooser Intent for a cached export file via FileProvider. */
    fun buildShareIntent(file: File): Intent {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("export", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Share export").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun fileName(): String = "simplelife_${LocalDate.now()}.json"

    private fun TaskEntity.toStatus(uid: String?) = TaskStatus(
        uid = uid,
        text = text,
        completed = completed,
        createdAt = isoLocal(createdAtMillis),
        completedAt = completedAtMillis?.let { isoLocal(it) },
        penaltyCount = penaltyCount
    )

    private fun isoLocal(millis: Long): String =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        )
}

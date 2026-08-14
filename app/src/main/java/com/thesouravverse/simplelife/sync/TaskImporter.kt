package com.thesouravverse.simplelife.sync

import android.content.Context
import android.net.Uri
import com.thesouravverse.simplelife.data.TaskRepository
import com.thesouravverse.simplelife.data.db.SyncedTaskDao
import com.thesouravverse.simplelife.data.db.SyncedTaskEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskImporter @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val syncedDao: SyncedTaskDao,
    private val taskRepo: TaskRepository
) {
    /** Returns inserted count, or throws with a readable message. */
    suspend fun importFrom(uri: Uri): Int = withContext(Dispatchers.IO) {
        val text = runCatching {
            ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull() ?: throw IllegalArgumentException("Couldn't open that file")

        val inbox = runCatching { SyncJson.decodeFromString<Inbox>(text) }
            .getOrElse { throw IllegalArgumentException("Not a valid tasks file") }

        var inserted = 0
        for (item in inbox.tasks) {
            if (item.uid.isBlank() || item.text.isBlank()) continue
            if (syncedDao.exists(item.uid)) continue
            val day = runCatching { LocalDate.parse(item.date) }.getOrNull() ?: continue
            val safeDay = if (day.isBefore(LocalDate.now())) LocalDate.now() else day
            val parentId = taskRepo.addTaskFromSync(safeDay, item.text, item.subtasks)
            syncedDao.insert(SyncedTaskEntity(uid = item.uid, localId = parentId))
            inserted++
        }
        inserted
    }
}

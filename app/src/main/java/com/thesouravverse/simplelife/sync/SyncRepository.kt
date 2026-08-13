package com.thesouravverse.simplelife.sync

import com.thesouravverse.simplelife.data.TaskRepository
import com.thesouravverse.simplelife.data.db.SyncedTaskDao
import com.thesouravverse.simplelife.data.db.SyncedTaskEntity
import com.thesouravverse.simplelife.data.db.TaskDao
import com.thesouravverse.simplelife.data.db.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val client: GitHubContentsClient,
    private val settings: SyncSettings,
    private val syncedDao: SyncedTaskDao,
    private val taskDao: TaskDao,
    private val taskRepo: TaskRepository
) {
    suspend fun pullInbox(): Int {
        if (!settings.isConfigured()) return 0
        val file = client.get(GitHubContentsClient.INBOX_PATH) ?: return 0
        return importInbox(file.text).coerceAtLeast(0)
    }

    /**
     * Import tasks from a raw inbox.json string (local file or remote).
     * Returns the number inserted, or -1 when the JSON can't be parsed.
     */
    suspend fun importInbox(text: String): Int {
        val inbox = runCatching { SyncJson.decodeFromString<Inbox>(text) }
            .getOrElse { return -1 }

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
        return inserted
    }

    suspend fun pushStatus(daysBack: Long = 14) {
        if (!settings.isConfigured()) return
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
            updatedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            totalXp = totalXp,
            days = days
        )
        val existing = client.get(GitHubContentsClient.STATUS_PATH)
        client.put(
            GitHubContentsClient.STATUS_PATH,
            SyncJson.encodeToString(report),
            existing?.sha,
            "status: ${report.updatedAt}"
        )
    }

    private fun TaskEntity.toStatus(uid: String?) = TaskStatus(
        uid = uid, text = text, completed = completed, penaltyCount = penaltyCount
    )
}

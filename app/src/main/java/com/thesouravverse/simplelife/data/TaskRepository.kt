package com.thesouravverse.simplelife.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.thesouravverse.simplelife.data.db.TaskDao
import com.thesouravverse.simplelife.data.db.TaskEntity
import com.thesouravverse.simplelife.widget.SimpleLifeWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val dao: TaskDao,
    @ApplicationContext private val context: Context
) {
    fun tasksForDay(day: LocalDate): Flow<List<TaskEntity>> =
        dao.tasksForDay(day.toEpochDay())

    /**
     * Total XP across all history.
     *  - Leaf top-level task complete = +10
     *  - Task with N sub-tasks: each completed sub contributes 10/N (parent itself is 0)
     *  - Every penaltyCount on a top-level task contributes -5
     */
    fun totalXpFlow(): Flow<Int> = dao.allTasksFlow().map { all ->
        val byParent = all.filter { it.parentId != null }.groupBy { it.parentId!! }
        var xp = 0.0
        for (t in all.filter { it.parentId == null }) {
            xp -= t.penaltyCount * 5
            val subs = byParent[t.id].orEmpty()
            if (subs.isEmpty()) {
                if (t.completed) xp += 10
            } else {
                val per = 10.0 / subs.size
                xp += subs.count { it.completed } * per
            }
        }
        xp.toInt()
    }

    suspend fun addTask(day: LocalDate, text: String) {
        if (text.isBlank()) return
        dao.insert(TaskEntity(dayEpochDay = day.toEpochDay(), text = text.trim()))
        refreshWidget()
    }

    suspend fun addSubtask(parent: TaskEntity, text: String) {
        if (text.isBlank()) return
        dao.insert(
            TaskEntity(
                dayEpochDay = parent.dayEpochDay,
                text = text.trim(),
                parentId = parent.id
            )
        )
        // A fresh (incomplete) sub-task should un-complete the parent if it was auto-completed.
        if (parent.completed) {
            dao.update(parent.copy(completed = false, completedAtMillis = null))
        }
        refreshWidget()
    }

    suspend fun toggleCompleted(task: TaskEntity) {
        val newCompleted = !task.completed
        val now = if (newCompleted) System.currentTimeMillis() else null
        dao.update(task.copy(completed = newCompleted, completedAtMillis = now))

        if (task.parentId != null) {
            // Toggled a sub-task → re-derive the parent's completion state.
            val parent = dao.getById(task.parentId) ?: run { refreshWidget(); return }
            val siblings = dao.subtasksOf(task.parentId)
            val allDone = siblings.all { s ->
                if (s.id == task.id) newCompleted else s.completed
            }
            if (allDone != parent.completed) {
                dao.update(
                    parent.copy(
                        completed = allDone,
                        completedAtMillis = if (allDone) System.currentTimeMillis() else null
                    )
                )
            }
        } else {
            // Toggled a parent that has sub-tasks → cascade to all subs.
            val subs = dao.subtasksOf(task.id)
            if (subs.isNotEmpty()) {
                val nowOrNull = if (newCompleted) System.currentTimeMillis() else null
                subs.forEach { s ->
                    if (s.completed != newCompleted) {
                        dao.update(s.copy(completed = newCompleted, completedAtMillis = nowOrNull))
                    }
                }
            }
        }
        refreshWidget()
    }

    suspend fun toggleById(id: Long) {
        val task = dao.getById(id) ?: return
        toggleCompleted(task)
    }

    suspend fun deleteTask(task: TaskEntity) {
        if (task.parentId == null) {
            dao.deleteSubtasksOf(task.id)
        }
        dao.deleteById(task.id)
        if (task.parentId != null) {
            val parent = dao.getById(task.parentId)
            if (parent != null) {
                val remaining = dao.subtasksOf(task.parentId)
                val allDone = remaining.isNotEmpty() && remaining.all { it.completed }
                if (allDone != parent.completed) {
                    dao.update(
                        parent.copy(
                            completed = allDone,
                            completedAtMillis = if (allDone) System.currentTimeMillis() else null
                        )
                    )
                }
            }
        }
        refreshWidget()
    }

    /** Pull every still-incomplete past task into today and bump its penalty counter. */
    suspend fun applyMissedPenalties(today: LocalDate = LocalDate.now()) {
        dao.rolloverIncomplete(today.toEpochDay())
        refreshWidget()
    }

    private suspend fun refreshWidget() {
        runCatching { SimpleLifeWidget().updateAll(context) }
    }
}

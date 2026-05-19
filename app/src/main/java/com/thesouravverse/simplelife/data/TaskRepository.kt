package com.thesouravverse.simplelife.data

import com.thesouravverse.simplelife.data.db.TaskDao
import com.thesouravverse.simplelife.data.db.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val dao: TaskDao
) {
    fun tasksForDay(day: LocalDate): Flow<List<TaskEntity>> =
        dao.tasksForDay(day.toEpochDay())

    /** Total XP across all history: +10 per completed, -5 per penalized. */
    fun totalXpFlow(): Flow<Int> =
        dao.completedCountFlow().combine(dao.penalizedCountFlow()) { done, missed ->
            done * 10 - missed * 5
        }

    suspend fun addTask(day: LocalDate, text: String) {
        if (text.isBlank()) return
        dao.insert(TaskEntity(dayEpochDay = day.toEpochDay(), text = text.trim()))
    }

    suspend fun toggleCompleted(task: TaskEntity) {
        val now = if (!task.completed) System.currentTimeMillis() else null
        dao.update(
            task.copy(
                completed = !task.completed,
                completedAtMillis = now,
                // If user re-checks a previously-penalized task, keep penalized flag
                // so total XP stays consistent (-5 stays, +10 added).
            )
        )
    }

    suspend fun toggleById(id: Long) {
        val task = dao.getById(id) ?: return
        toggleCompleted(task)
    }

    suspend fun deleteTask(task: TaskEntity) = dao.deleteById(task.id)

    suspend fun applyMissedPenalties(today: LocalDate = LocalDate.now()) {
        dao.applyMissedPenalties(today.toEpochDay())
    }
}

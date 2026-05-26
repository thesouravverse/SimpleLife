package com.thesouravverse.simplelife.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    /** All tasks for a given day — both top-level and sub-tasks. */
    @Query("SELECT * FROM tasks WHERE dayEpochDay = :day ORDER BY createdAtMillis ASC")
    fun tasksForDay(day: Long): Flow<List<TaskEntity>>

    /** Stream of every task in the database — used to compute global XP. */
    @Query("SELECT * FROM tasks")
    fun allTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE parentId = :parentId")
    suspend fun subtasksOf(parentId: Long): List<TaskEntity>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tasks WHERE parentId = :parentId")
    suspend fun deleteSubtasksOf(parentId: Long)

    /** Mark every parent that's still incomplete and lives on a past day as
     *  rolled over: bump its penaltyCount and pull it to today. */
    @Query("""
        UPDATE tasks
        SET penaltyCount = penaltyCount + 1,
            dayEpochDay = :today
        WHERE completed = 0
          AND parentId IS NULL
          AND dayEpochDay < :today
    """)
    suspend fun rolloverParents(today: Long)

    /** Keep every sub-task's day in sync with its parent. Cheap; run after parent moves. */
    @Query("""
        UPDATE tasks
        SET dayEpochDay = (
            SELECT p.dayEpochDay FROM tasks p WHERE p.id = tasks.parentId
        )
        WHERE parentId IS NOT NULL
    """)
    suspend fun syncSubtaskDays()

    @Transaction
    suspend fun rolloverIncomplete(today: Long) {
        rolloverParents(today)
        syncSubtaskDays()
    }
}

package com.thesouravverse.simplelife.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE dayEpochDay = :day ORDER BY createdAtMillis ASC")
    fun tasksForDay(day: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT COUNT(*) FROM tasks WHERE completed = 1")
    fun completedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE penalized = 1")
    fun penalizedCountFlow(): Flow<Int>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Mark all unchecked tasks from days strictly before [today] as penalized,
     * if they haven't already been. Idempotent.
     */
    @Query("""
        UPDATE tasks
        SET penalized = 1
        WHERE completed = 0
          AND penalized = 0
          AND dayEpochDay < :today
    """)
    suspend fun applyMissedPenalties(today: Long)
}

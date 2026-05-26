package com.thesouravverse.simplelife.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["dayEpochDay"]),
        Index(value = ["parentId"])
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayEpochDay: Long,            // LocalDate.toEpochDay()
    val text: String,
    val completed: Boolean = false,
    val completedAtMillis: Long? = null,
    /** Number of times this task has rolled over un-completed. -5 XP per count. */
    val penaltyCount: Int = 0,
    /** When set, this row is a sub-task of the parent task with this id. */
    val parentId: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

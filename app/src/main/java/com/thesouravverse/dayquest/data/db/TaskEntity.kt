package com.thesouravverse.dayquest.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [Index(value = ["dayEpochDay"])]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayEpochDay: Long,           // LocalDate.toEpochDay()
    val text: String,
    val completed: Boolean = false,
    val completedAtMillis: Long? = null,
    val penalized: Boolean = false,  // -5 already applied for this task
    val createdAtMillis: Long = System.currentTimeMillis()
)

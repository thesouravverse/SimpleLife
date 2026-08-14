package com.thesouravverse.simplelife.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal val SyncJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}

@Serializable
data class Inbox(
    val version: Int = 1,
    val generatedAt: String? = null,
    val tasks: List<InboxTask> = emptyList()
)

@Serializable
data class InboxTask(
    val uid: String,
    val date: String,
    val text: String,
    val subtasks: List<String> = emptyList()
)

@Serializable
data class StatusReport(
    val version: Int = 1,
    val updatedAt: String,
    val totalXp: Int,
    val days: List<DayStatus>
)

@Serializable
data class DayStatus(val date: String, val tasks: List<TaskStatus>)

@Serializable
data class TaskStatus(
    val uid: String? = null,
    val text: String,
    val completed: Boolean,
    val createdAt: String? = null,
    val completedAt: String? = null,
    val penaltyCount: Int,
    val subtasks: List<TaskStatus> = emptyList()
)

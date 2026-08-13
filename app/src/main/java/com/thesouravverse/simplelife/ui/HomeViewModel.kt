package com.thesouravverse.simplelife.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thesouravverse.simplelife.data.TaskRepository
import com.thesouravverse.simplelife.data.db.TaskEntity
import com.thesouravverse.simplelife.sync.SyncRepository
import com.thesouravverse.simplelife.sync.SyncSettings
import com.thesouravverse.simplelife.work.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: TaskRepository,
    private val syncRepo: SyncRepository,
    private val syncSettings: SyncSettings,
    private val workScheduler: WorkScheduler
) : ViewModel() {

    private val _selectedDay = MutableStateFlow(LocalDate.now())
    val selectedDay: StateFlow<LocalDate> = _selectedDay

    val tasks: StateFlow<List<TaskEntity>> = _selectedDay
        .flatMapLatest { repo.tasksForDay(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalXp: StateFlow<Int> = repo.totalXpFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val token: StateFlow<String> = syncSettings.tokenFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val repoName: StateFlow<String> = syncSettings.repoFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val lastResult: StateFlow<String> = syncSettings.lastResultFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    init {
        // Apply -5 penalty for any past unchecked tasks every time the app opens.
        // This handles the case where the device was off at 23:59 yesterday.
        viewModelScope.launch { repo.applyMissedPenalties() }
    }

    fun selectDay(day: LocalDate) {
        _selectedDay.value = day
    }

    fun addTask(text: String) {
        viewModelScope.launch { repo.addTask(_selectedDay.value, text) }
    }

    fun addSubtask(parent: TaskEntity, text: String) {
        viewModelScope.launch { repo.addSubtask(parent, text) }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch { repo.toggleCompleted(task) }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { repo.deleteTask(task) }
    }

    fun saveSyncSettings(token: String, repo: String) {
        viewModelScope.launch {
            syncSettings.save(token, repo)
            workScheduler.syncNow()
        }
    }

    fun syncNow() {
        workScheduler.syncNow()
    }

    /** Import tasks from a raw inbox.json string picked from device storage. */
    fun importJson(text: String) {
        viewModelScope.launch {
            val n = syncRepo.importInbox(text)
            val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            val msg = when {
                n < 0 -> "Import failed \u00b7 not valid JSON \u00b7 $time"
                else -> "Loaded $n new task${if (n == 1) "" else "s"} from file \u00b7 $time"
            }
            syncSettings.setLastResult(msg)
        }
    }
}

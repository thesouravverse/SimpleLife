package com.thesouravverse.simplelife.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thesouravverse.simplelife.data.TaskRepository
import com.thesouravverse.simplelife.data.db.TaskEntity
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
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: TaskRepository,
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
}

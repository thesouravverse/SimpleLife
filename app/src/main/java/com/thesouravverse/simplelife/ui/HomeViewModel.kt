package com.thesouravverse.simplelife.ui

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thesouravverse.simplelife.data.TaskRepository
import com.thesouravverse.simplelife.data.db.TaskEntity
import com.thesouravverse.simplelife.sync.SyncSettings
import com.thesouravverse.simplelife.sync.TaskExporter
import com.thesouravverse.simplelife.sync.TaskImporter
import com.thesouravverse.simplelife.work.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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
    private val importer: TaskImporter,
    private val exporter: TaskExporter,
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

    /** One-shot messages for the home-screen Snackbar. */
    private val _snackbar = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbar: SharedFlow<String> = _snackbar

    /** One-shot share-sheet intents for exporting. */
    private val _shareIntent = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val shareIntent: SharedFlow<Intent> = _shareIntent

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

    /** Import tasks from a picked or opened .json Uri. Emits a Snackbar message. */
    fun import(uri: Uri) {
        viewModelScope.launch {
            val msg = try {
                val n = importer.importFrom(uri)
                when {
                    n == 0 -> "Nothing new — already imported"
                    else -> "Added $n task${if (n == 1) "" else "s"}"
                }
            } catch (e: Exception) {
                "Couldn't read that file"
            }
            _snackbar.emit(msg)
        }
    }

    /** Build today's export, persist it to shared storage, and open the share sheet. */
    fun exportTasks() {
        viewModelScope.launch {
            try {
                runCatching { exporter.writeToSharedStorage() }
                val file = exporter.writeToCache()
                _shareIntent.emit(exporter.buildShareIntent(file))
            } catch (e: Exception) {
                _snackbar.emit("Couldn't export")
            }
        }
    }
}

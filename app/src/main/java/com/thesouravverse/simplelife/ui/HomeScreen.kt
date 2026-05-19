package com.thesouravverse.simplelife.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thesouravverse.simplelife.data.db.TaskEntity
import com.thesouravverse.simplelife.ui.theme.DqPalette
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel = hiltViewModel()) {
    val day by vm.selectedDay.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val xp by vm.totalXp.collectAsStateWithLifecycle()

    val today = LocalDate.now()
    val isToday = day == today
    val isFuture = day.isAfter(today)

    var showAdd by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<TaskEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            if (isToday) {
                FloatingActionButton(
                    onClick = { showAdd = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add task", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Top bar: prev / date+xp+badge / next + calendar
            DayHeader(
                day = day,
                xp = xp,
                onPrev = { vm.selectDay(day.minusDays(1)) },
                onNext = { vm.selectDay(day.plusDays(1)) },
                canGoNext = !isToday && !isFuture,
                onCalendar = { showCalendar = true }
            )

            Spacer(Modifier.height(8.dp))

            if (tasks.isEmpty()) {
                EmptyState(isToday = isToday, isFuture = isFuture)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 8.dp, bottom = 96.dp
                    )
                ) {
                    items(items = tasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            readOnly = !isToday,
                            onToggle = { vm.toggleTask(task) },
                            onDelete = { pendingDelete = task }
                        )
                    }
                }
            }
        }
    }

    // Add task sheet
    if (showAdd) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAdd = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            AddTaskSheet(
                onAdd = { text ->
                    vm.addTask(text)
                    showAdd = false
                },
                onCancel = { showAdd = false }
            )
        }
    }

    // Calendar picker
    if (showCalendar) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = day.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli(),
            yearRange = (today.year - 5)..today.year
        )
        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val picked = java.time.Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        if (!picked.isAfter(today)) vm.selectDay(picked)
                    }
                    showCalendar = false
                }) { Text("Jump") }
            },
            dismissButton = {
                TextButton(onClick = { showCalendar = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = state) }
    }

    // Delete confirm
    pendingDelete?.let { t ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteTask(t)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
            title = { Text("Delete this task?") },
            text = { Text("\u201C${t.text}\u201D will be removed.") }
        )
    }
}

@Composable
private fun DayHeader(
    day: LocalDate,
    xp: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    canGoNext: Boolean,
    onCalendar: () -> Unit
) {
    val badge = Badge.forXp(xp)
    val next = Badge.nextFor(xp)
    val today = LocalDate.now()
    val label = when (day) {
        today -> "TODAY"
        today.minusDays(1) -> "YESTERDAY"
        else -> day.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())).uppercase()
    }
    val pretty = day.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Row: < label/date > + calendar
        Row(verticalAlignment = Alignment.CenterVertically) {
            NavArrow(Icons.Default.ChevronLeft, "Previous day", onClick = onPrev)
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    pretty,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            NavArrow(Icons.Default.ChevronRight, "Next day", onClick = onNext, enabled = canGoNext)
            IconButton(onClick = onCalendar) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Pick date")
            }
        }

        Spacer(Modifier.height(14.dp))

        // Badge card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(badge.emoji, fontSize = 36.sp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    badge.title.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "$xp XP",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Black
                )
                if (next != null) {
                    val span = (next.threshold - badge.threshold).coerceAtLeast(1)
                    val into = (xp - badge.threshold).coerceIn(0, span)
                    val progress = into.toFloat() / span.toFloat()
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        gapSize = 0.dp,
                        drawStopIndicator = {}
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${next.threshold - xp} XP to ${next.title} ${next.emoji}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Top tier reached. Glory is yours.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NavArrow(
    icon: ImageVector,
    desc: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            icon,
            contentDescription = desc,
            tint = if (enabled) MaterialTheme.colorScheme.onBackground
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    readOnly: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(enabled = !readOnly) { onToggle() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckCircle(checked = task.completed)
        Spacer(Modifier.width(14.dp))
        Text(
            text = task.text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (task.completed)
                MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onBackground,
            textDecoration = if (task.completed) TextDecoration.LineThrough else null
        )
        if (task.completed) {
            Text(
                "+10",
                color = DqPalette.Success,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        } else if (task.penalized) {
            Text(
                "-5",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        if (!readOnly) {
            IconButton(onClick = onDelete, modifier = Modifier.padding(start = 4.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun CheckCircle(checked: Boolean) {
    val bg = if (checked) DqPalette.Success else Color.Transparent
    val border = if (checked) DqPalette.Success else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(bg, CircleShape)
            .border(2.dp, border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = checked, enter = fadeIn(), exit = fadeOut()) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskSheet(onAdd: (String) -> Unit, onCancel: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val kb = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            "What's the task?",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus),
            placeholder = { Text("e.g. Drink water") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Spacer(Modifier.width(8.dp))
            FloatingActionButton(
                onClick = {
                    kb?.hide()
                    onAdd(text)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun EmptyState(isToday: Boolean, isFuture: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when {
                    isFuture -> "\uD83D\uDD2E"
                    isToday -> "\u2728"
                    else -> "\uD83D\uDCDD"
                },
                fontSize = 64.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                when {
                    isFuture -> "Tomorrow is unwritten."
                    isToday -> "A blank page.\nTap + to start your day."
                    else -> "No tasks on this day."
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

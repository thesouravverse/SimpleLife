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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
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
fun HomeScreen(
    vm: HomeViewModel = hiltViewModel(),
    triggerAdd: Boolean = false,
    onAddConsumed: () -> Unit = {}
) {
    val day by vm.selectedDay.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val xp by vm.totalXp.collectAsStateWithLifecycle()

    val today = LocalDate.now()
    val isToday = day == today

    var showAdd by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    var badgeExpanded by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<TaskEntity?>(null) }

    LaunchedEffect(triggerAdd) {
        if (triggerAdd) {
            if (!isToday) vm.selectDay(today)
            showAdd = true
            onAddConsumed()
        }
    }

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
            TopBar(
                day = day,
                isToday = isToday,
                xp = xp,
                badgeExpanded = badgeExpanded,
                onBadgeTap = { badgeExpanded = !badgeExpanded },
                onDateTap = { showCalendar = true },
                onBackToToday = { vm.selectDay(today) }
            )

            BadgeDetail(xp = xp, visible = badgeExpanded)

            Spacer(Modifier.height(4.dp))

            if (tasks.isEmpty()) {
                EmptyState(isToday = isToday)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
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
                }) { Text("Open") }
            },
            dismissButton = {
                TextButton(onClick = { showCalendar = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = state) }
    }

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
private fun TopBar(
    day: LocalDate,
    isToday: Boolean,
    xp: Int,
    badgeExpanded: Boolean,
    onBadgeTap: () -> Unit,
    onDateTap: () -> Unit,
    onBackToToday: () -> Unit
) {
    val badge = Badge.forXp(xp)
    val dateLabel = day.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: date or "Today" chip
        if (isToday) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable { onDateTap() }
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            )
        } else {
            AssistChip(
                onClick = onBackToToday,
                label = { Text("Today", fontSize = 12.sp) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }

        Spacer(Modifier.weight(1f))

        // Middle: small circular badge
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .clickable { onBadgeTap() },
            contentAlignment = Alignment.Center
        ) {
            Text(badge.emoji, fontSize = 22.sp)
        }

        Spacer(Modifier.weight(1f))

        // Right: calendar
        IconButton(
            onClick = onDateTap,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = "Open calendar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun BadgeDetail(xp: Int, visible: Boolean) {
    val badge = Badge.forXp(xp)
    val next = Badge.nextFor(xp)

    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                badge.title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            Text(
                "$xp XP",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(6.dp))
            if (next != null) {
                val span = (next.threshold - badge.threshold).coerceAtLeast(1)
                val into = (xp - badge.threshold).coerceIn(0, span)
                val progress = into.toFloat() / span.toFloat()
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${next.threshold - xp} XP to ${next.title} ${next.emoji}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            } else {
                Text(
                    "Top tier reached. Glory is yours.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
            color = MaterialTheme.colorScheme.onBackground
            // intentionally no strikethrough — user wants to re-read completed tasks
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
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
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
            .size(30.dp)
            .background(bg, CircleShape)
            .border(2.dp, border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = checked, enter = fadeIn(), exit = fadeOut()) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
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
private fun EmptyState(isToday: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (isToday) "A blank page.\nTap + to start." else "No tasks on this day.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

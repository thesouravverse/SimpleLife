package com.thesouravverse.simplelife.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thesouravverse.simplelife.MainActivity
import com.thesouravverse.simplelife.data.db.TaskEntity
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate

class SimpleLifeWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val addTaskIntent = Intent(context, com.thesouravverse.simplelife.QuickAddActivity::class.java).apply {
            action = "com.thesouravverse.simplelife.OPEN_ADD_TASK"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val repo = EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .taskRepository()

        provideContent {
            GlanceTheme {
                val today = LocalDate.now()
                val tasks by repo.tasksForDay(today)
                    .collectAsState(initial = emptyList())
                WidgetBody(tasks, openAppIntent, addTaskIntent)
            }
        }
    }

    @Composable
    private fun WidgetBody(
        tasks: List<TaskEntity>,
        openAppIntent: Intent,
        addTaskIntent: Intent
    ) {
        val done = tasks.count { it.completed }
        val total = tasks.size
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .cornerRadius(20.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clickable(actionStartActivity(openAppIntent))
            ) {
                // Header: "Today  X / N"
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onSurfaceVariant
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                if (total > 0) {
                    Text(
                        text = "$done / $total",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.primary
                        )
                    )
                }
            }
            Spacer(GlanceModifier.height(8.dp))

            if (tasks.isEmpty()) {
                EmptyWidget()
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(items = tasks, itemId = { it.id }) { task ->
                        WidgetTaskRow(task)
                    }
                }
            }
            } // close inner Column

            // Quick-add FAB, bottom-right (subtle, blends with widget surface)
            Box(
                modifier = GlanceModifier
                    .size(44.dp)
                    .cornerRadius(22.dp)
                    .background(GlanceTheme.colors.surfaceVariant)
                    .clickable(actionStartActivity(addTaskIntent)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface
                    )
                )
            }
        }
    }

    @Composable
    private fun WidgetTaskRow(task: TaskEntity) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clickable(
                    actionRunCallback<ToggleTaskAction>(
                        parameters = ToggleTaskAction.params(task.id)
                    )
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox circle
            val checkColor = if (task.completed) Color(0xFF2BB673) else Color(0x66888888)
            Box(
                modifier = GlanceModifier
                    .size(20.dp)
                    .cornerRadius(10.dp)
                    .background(if (task.completed) Color(0xFF2BB673) else Color.Transparent)
                    .padding(0.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (task.completed) "✓" else "○",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(
                            if (task.completed) Color.White else checkColor
                        )
                    )
                )
            }
            Spacer(GlanceModifier.width(10.dp))
            Text(
                text = task.text,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = if (task.completed) FontWeight.Normal else FontWeight.Medium,
                    color = GlanceTheme.colors.onSurface
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight()
            )
            if (task.completed) {
                Text(
                    text = "+10",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color(0xFF2BB673))
                    )
                )
            }
        }
    }

    @Composable
    private fun EmptyWidget() {
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No tasks yet.\nTap to add one.",
                style = TextStyle(
                    fontSize = 13.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
        }
    }
}

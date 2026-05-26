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
        val settingsIntent = Intent(context, WidgetSettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val repo = EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .taskRepository()

        val opacity = WidgetSettings.getOpacity(context)
        val isDark = (context.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        val baseBg = if (isDark) Color(0xFF14120F) else Color(0xFFFAF6EE)
        val widgetBg = baseBg.copy(alpha = opacity)

        provideContent {
            GlanceTheme {
                val today = LocalDate.now()
                val allToday by repo.tasksForDay(today)
                    .collectAsState(initial = emptyList())
                WidgetBody(allToday, openAppIntent, settingsIntent, widgetBg)
            }
        }
    }

    private data class WidgetRow(
        val task: TaskEntity,
        val isSub: Boolean,
        val xpLabel: String?
    )

    @Composable
    private fun WidgetBody(
        allTasks: List<TaskEntity>,
        openAppIntent: Intent,
        settingsIntent: Intent,
        bgColor: Color
    ) {
        val parents = allTasks.filter { it.parentId == null }
        val subsByParent = allTasks.filter { it.parentId != null }.groupBy { it.parentId!! }
        val done = parents.count { it.completed }
        val total = parents.size

        // Build flat display list: each parent, then its subs indented.
        val rows = buildList {
            for (p in parents) {
                val subs = subsByParent[p.id].orEmpty()
                val parentXp = when {
                    subs.isEmpty() && p.completed -> "+10"
                    p.penaltyCount > 0 -> "-${p.penaltyCount * 5}"
                    else -> null
                }
                add(WidgetRow(p, isSub = false, xpLabel = parentXp))
                if (subs.isNotEmpty()) {
                    val xpPer = 10 / subs.size.coerceAtLeast(1)
                    for (s in subs) {
                        add(WidgetRow(s, isSub = true, xpLabel = if (s.completed) "+$xpPer" else null))
                    }
                }
            }
        }
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgColor)
                .cornerRadius(20.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clickable(actionStartActivity(openAppIntent))
            ) {
                // Header: "Today  X / N  ⋯"
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
                            ),
                            modifier = GlanceModifier.padding(end = 8.dp)
                        )
                    }
                    // Small, discreet "⋯" — opens widget settings (opacity)
                    Text(
                        text = "⋯",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        ),
                        modifier = GlanceModifier
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .clickable(actionStartActivity(settingsIntent))
                    )
                }
                Spacer(GlanceModifier.height(8.dp))

                if (rows.isEmpty()) {
                    EmptyWidget()
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                        items(items = rows, itemId = { (it.task.id * 2L) + if (it.isSub) 1L else 0L }) { row ->
                            WidgetTaskRow(row)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun WidgetTaskRow(row: WidgetRow) {
        val task = row.task
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(
                    start = if (row.isSub) 22.dp else 0.dp,
                    top = if (row.isSub) 3.dp else 6.dp,
                    bottom = if (row.isSub) 3.dp else 6.dp
                )
                .clickable(
                    actionRunCallback<ToggleTaskAction>(
                        parameters = ToggleTaskAction.params(task.id)
                    )
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dotSize = if (row.isSub) 16.dp else 20.dp
            val checkColor = if (task.completed) Color(0xFF2BB673) else Color(0x66888888)
            Box(
                modifier = GlanceModifier
                    .size(dotSize)
                    .cornerRadius(dotSize / 2)
                    .background(if (task.completed) Color(0xFF2BB673) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (task.completed) "✓" else "○",
                    style = TextStyle(
                        fontSize = if (row.isSub) 11.sp else 14.sp,
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
                    fontSize = if (row.isSub) 12.sp else 14.sp,
                    fontWeight = if (task.completed || row.isSub) FontWeight.Normal else FontWeight.Medium,
                    color = if (row.isSub) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight()
            )
            row.xpLabel?.let { label ->
                val color = if (label.startsWith("-")) Color(0xFFE07A5F) else Color(0xFF2BB673)
                Text(
                    text = label,
                    style = TextStyle(
                        fontSize = if (row.isSub) 11.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(color)
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

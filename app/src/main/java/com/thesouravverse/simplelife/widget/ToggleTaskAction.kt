package com.thesouravverse.simplelife.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.android.EntryPointAccessors

/**
 * Tapped from the home-screen widget to toggle a task's completion.
 * The Room flow update auto-recomposes both the app and the widget.
 */
class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val id = parameters[taskIdKey] ?: return
        val repo = EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .taskRepository()
        repo.toggleById(id)
        SimpleLifeWidget().update(context, glanceId)
    }

    companion object {
        val taskIdKey = ActionParameters.Key<Long>("task_id")
        fun params(id: Long) = actionParametersOf(taskIdKey to id)
    }
}

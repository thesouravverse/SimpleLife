package com.thesouravverse.simplelife.widget

import com.thesouravverse.simplelife.data.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt entry point so Glance widget components (which can't use @Inject)
 * can pull dependencies from the singleton graph.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun taskRepository(): TaskRepository
}

package com.thesouravverse.simplelife.widget

import android.content.Context
import androidx.core.content.edit

/**
 * Lightweight on-device settings for the home-screen widget.
 * Backed by SharedPreferences — no DataStore overhead for one float.
 */
object WidgetSettings {
    private const val PREFS = "simplelife_widget_prefs"
    private const val KEY_OPACITY = "opacity"
    const val DEFAULT_OPACITY = 0.92f
    const val MIN_OPACITY = 0.20f
    const val MAX_OPACITY = 1.0f

    fun getOpacity(context: Context): Float =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getFloat(KEY_OPACITY, DEFAULT_OPACITY)
            .coerceIn(MIN_OPACITY, MAX_OPACITY)

    fun setOpacity(context: Context, value: Float) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putFloat(KEY_OPACITY, value.coerceIn(MIN_OPACITY, MAX_OPACITY)) }
    }
}

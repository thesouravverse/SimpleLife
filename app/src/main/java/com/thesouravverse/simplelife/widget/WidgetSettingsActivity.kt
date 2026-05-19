package com.thesouravverse.simplelife.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import com.thesouravverse.simplelife.ui.theme.SimpleLifeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Translucent settings dialog for the widget — currently just an opacity slider.
 * Opens when user taps the small "•" indicator on the widget.
 */
class WidgetSettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initial = WidgetSettings.getOpacity(this)
        setContent {
            SimpleLifeTheme {
                var value by remember { mutableFloatStateOf(initial) }
                AlertDialog(
                    onDismissRequest = { finish() },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    title = {
                        Text(
                            "Widget background",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Opacity  ${(value * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Slider(
                                value = value,
                                onValueChange = { value = it },
                                valueRange = WidgetSettings.MIN_OPACITY..WidgetSettings.MAX_OPACITY
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            WidgetSettings.setOpacity(this@WidgetSettingsActivity, value)
                            CoroutineScope(Dispatchers.Main).launch {
                                runCatching {
                                    SimpleLifeWidget().updateAll(this@WidgetSettingsActivity)
                                }
                                finish()
                            }
                        }) { Text("Save", fontWeight = FontWeight.SemiBold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { finish() }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

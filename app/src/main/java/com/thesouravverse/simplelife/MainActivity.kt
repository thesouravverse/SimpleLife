package com.thesouravverse.simplelife

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.thesouravverse.simplelife.ui.HomeScreen
import com.thesouravverse.simplelife.ui.theme.SimpleLifeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var openAddRequest by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeAddIntent(intent)
        setContent {
            SimpleLifeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(
                        triggerAdd = openAddRequest,
                        onAddConsumed = { openAddRequest = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeAddIntent(intent)
    }

    private fun consumeAddIntent(i: Intent?) {
        if (i?.getBooleanExtra(EXTRA_OPEN_ADD, false) == true) {
            openAddRequest = true
            i.removeExtra(EXTRA_OPEN_ADD)
        }
    }

    companion object {
        const val EXTRA_OPEN_ADD = "open_add"
    }
}

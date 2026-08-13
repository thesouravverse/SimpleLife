package com.thesouravverse.simplelife.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncDataStore by preferencesDataStore(name = "sync_settings")

/** Token + repo live on-device only. Never compiled into the APK. */
@Singleton
class SyncSettings @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val REPO = stringPreferencesKey("repo")
        val LAST_RESULT = stringPreferencesKey("last_result")
    }

    val tokenFlow: Flow<String> = ctx.syncDataStore.data.map { it[Keys.TOKEN].orEmpty() }
    val repoFlow: Flow<String> = ctx.syncDataStore.data.map { it[Keys.REPO].orEmpty() }
    val lastResultFlow: Flow<String> = ctx.syncDataStore.data.map { it[Keys.LAST_RESULT].orEmpty() }

    suspend fun token(): String = tokenFlow.first()
    suspend fun repo(): String = repoFlow.first()
    suspend fun isConfigured(): Boolean = token().isNotBlank() && repo().isNotBlank()

    suspend fun save(token: String, repo: String) {
        ctx.syncDataStore.edit {
            it[Keys.TOKEN] = token.trim()
            it[Keys.REPO] = repo.trim()
        }
    }

    suspend fun setLastResult(text: String) {
        ctx.syncDataStore.edit { it[Keys.LAST_RESULT] = text }
    }
}

package com.thesouravverse.simplelife.sync

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubContentsClient @Inject constructor(
    private val settings: SyncSettings
) {
    data class RemoteFile(val text: String, val sha: String)

    /** Returns null when the file does not exist yet (404). */
    suspend fun get(path: String): RemoteFile? = withContext(Dispatchers.IO) {
        val conn = open("GET", path)
        try {
            when (val code = conn.responseCode) {
                200 -> {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val obj = SyncJson.parseToJsonElement(body) as JsonObject
                    val b64 = obj["content"]?.jsonPrimitive?.content.orEmpty()
                    val sha = obj["sha"]?.jsonPrimitive?.content.orEmpty()
                    RemoteFile(String(Base64.decode(b64, Base64.DEFAULT), Charsets.UTF_8), sha)
                }
                404 -> null
                else -> throw IOException("GET $path -> $code ${errorBody(conn)}")
            }
        } finally { conn.disconnect() }
    }

    suspend fun put(path: String, text: String, sha: String?, message: String) =
        withContext(Dispatchers.IO) {
            val payload = buildJsonObject {
                put("message", message)
                put("content", Base64.encodeToString(text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
                if (sha != null) put("sha", sha)
            }
            val conn = open("PUT", path).apply {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            try {
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                if (code !in 200..299) throw IOException("PUT $path -> $code ${errorBody(conn)}")
            } finally { conn.disconnect() }
        }

    private suspend fun open(method: String, path: String): HttpURLConnection {
        val repo = settings.repo()
        val token = settings.token()
        val url = URL("https://api.github.com/repos/$repo/contents/$path")
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "SimpleLife-Android")
        }
    }

    private fun errorBody(conn: HttpURLConnection): String =
        runCatching { conn.errorStream?.bufferedReader()?.use { it.readText() } }
            .getOrNull().orEmpty().take(300)

    companion object {
        const val INBOX_PATH = "inbox.json"
        const val STATUS_PATH = "status.json"
    }
}

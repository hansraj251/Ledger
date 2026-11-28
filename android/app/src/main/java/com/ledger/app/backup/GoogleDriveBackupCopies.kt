package com.ledger.app.backup

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoogleDriveBackupCopies(context: Context) {
    data class BackupCopy(
        val fileId: String,
        val name: String,
        val modifiedTime: String
    )

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val filesUrl = "https://www.googleapis.com/drive/v3/files"

    fun listBackups(accessToken: String, callback: (Result<List<BackupCopy>>) -> Unit) {
        Thread {
            try {
                val all = mutableListOf<BackupCopy>()
                var pageToken: String? = null
                do {
                    val query = "name contains 'Ledger Backup' and trashed = false and 'appDataFolder' in parents"
                    val url = buildString {
                        append(filesUrl)
                        append("?spaces=appDataFolder&pageSize=100&orderBy=modifiedTime%20desc")
                        append("&fields=nextPageToken,files(id,name,modifiedTime)")
                        append("&q=").append(URLEncoder.encode(query, "UTF-8"))
                        if (!pageToken.isNullOrBlank()) {
                            append("&pageToken=").append(URLEncoder.encode(pageToken, "UTF-8"))
                        }
                    }
                    val connection = URL(url).openConnection() as HttpURLConnection
                    try {
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 30_000
                        connection.readTimeout = 60_000
                        connection.setRequestProperty("Authorization", "Bearer $accessToken")
                        val code = connection.responseCode
                        if (code !in 200..299) {
                            val body = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                            throw IllegalStateException("Google Drive list failed (HTTP $code): $body")
                        }
                        val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                        val files = json.optJSONArray("files")
                        if (files != null) for (i in 0 until files.length()) {
                            val item = files.optJSONObject(i) ?: continue
                            val id = item.optString("id")
                            val name = item.optString("name")
                            val modified = item.optString("modifiedTime")
                            if (id.isNotBlank() && name.startsWith("Ledger Backup") && modified.isNotBlank()) {
                                all += BackupCopy(id, name, modified)
                            }
                        }
                        pageToken = json.optString("nextPageToken").takeIf { it.isNotBlank() && it != "null" }
                    } finally {
                        connection.disconnect()
                    }
                } while (pageToken != null)
                mainHandler.post { callback(Result.success(all.sortedByDescending { it.modifiedTime })) }
            } catch (error: Exception) {
                mainHandler.post { callback(Result.failure(error)) }
            }
        }.start()
    }

    fun deleteCopy(accessToken: String, copy: BackupCopy, callback: (Result<Unit>) -> Unit) {
        // Fail closed: only timestamped copies created by this feature may be deleted.
        if (!copy.name.startsWith("Ledger Backup Copy ") || copy.name == "Ledger Backup.db") {
            callback(Result.failure(IllegalArgumentException("The original Ledger backup cannot be deleted.")))
            return
        }
        Thread {
            try {
                val encodedId = URLEncoder.encode(copy.fileId, "UTF-8")
                val connection = URL("$filesUrl/$encodedId")
                    .openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "DELETE"
                    connection.connectTimeout = 30_000
                    connection.readTimeout = 60_000
                    connection.setRequestProperty("Authorization", "Bearer $accessToken")
                    val code = connection.responseCode
                    if (code !in 200..299) {
                        val body = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                        throw IllegalStateException("Google Drive delete failed (HTTP $code): $body")
                    }
                    mainHandler.post { callback(Result.success(Unit)) }
                } finally {
                    connection.disconnect()
                }
            } catch (error: Exception) {
                mainHandler.post { callback(Result.failure(error)) }
            }
        }.start()
    }

    fun createCopy(accessToken: String, callback: (Result<BackupCopy>) -> Unit) {
        listBackups(accessToken) { listResult ->
            listResult.onFailure { callback(Result.failure(it)) }
            listResult.onSuccess { backups ->
                val source = backups.firstOrNull { it.name == "Ledger Backup.db" }
                    ?: backups.firstOrNull()
                if (source == null) {
                    callback(Result.failure(IllegalStateException("No existing Ledger backup was found on Google Drive.")))
                    return@onSuccess
                }
                Thread {
                    try {
                        val stamp = SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.US).format(Date())
                        val copyName = "Ledger Backup Copy $stamp.db"
                        val encodedId = URLEncoder.encode(source.fileId, "UTF-8")
                        val connection = URL("$filesUrl/$encodedId/copy?fields=id,name,modifiedTime")
                            .openConnection() as HttpURLConnection
                        try {
                            connection.requestMethod = "POST"
                            connection.doOutput = true
                            connection.connectTimeout = 30_000
                            connection.readTimeout = 60_000
                            connection.setRequestProperty("Authorization", "Bearer $accessToken")
                            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                            connection.outputStream.use { it.write(JSONObject().put("name", copyName).toString().toByteArray(Charsets.UTF_8)) }
                            val code = connection.responseCode
                            if (code !in 200..299) {
                                val body = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                                throw IllegalStateException("Google Drive copy failed (HTTP $code): $body")
                            }
                            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                            val copy = BackupCopy(
                                json.getString("id"),
                                json.optString("name", copyName),
                                json.getString("modifiedTime")
                            )
                            mainHandler.post { callback(Result.success(copy)) }
                        } finally {
                            connection.disconnect()
                        }
                    } catch (error: Exception) {
                        mainHandler.post { callback(Result.failure(error)) }
                    }
                }.start()
            }
        }
    }
}

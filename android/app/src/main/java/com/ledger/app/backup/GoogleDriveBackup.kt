package com.ledger.app.backup

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

class GoogleDriveBackup(
    context: Context
) {

    companion object {
        private const val PREFERENCES_NAME = "ledger_backup"
        private const val FILE_ID_KEY = "google_drive_file_id"
        private const val BACKUP_FILE_NAME = "Ledger Backup.db"
        private const val MIME_TYPE = "application/x-sqlite3"
        private const val DRIVE_UPLOAD_URL =
            "https://www.googleapis.com/upload/drive/v3/files"
        private const val DRIVE_FILE_URL =
            "https://www.googleapis.com/drive/v3/files"
    }

    private val context = context.applicationContext
    private val preferences =
        this.context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
    private val mainHandler = Handler(Looper.getMainLooper())

    fun sync(
        database: SupportSQLiteDatabase,
        accessToken: String,
        callback: (Result<Unit>) -> Unit
    ) {
        Thread {
            var snapshot: File? = null

            try {
                snapshot = createDatabaseSnapshot(database)

                val storedFileId =
                    preferences.getString(
                        FILE_ID_KEY,
                        null
                    )

                val fileId =
                    if (storedFileId.isNullOrBlank()) {
                        uploadNewFile(
                            snapshot,
                            accessToken
                        )
                    } else {
                        try {
                            updateExistingFile(
                                storedFileId,
                                snapshot,
                                accessToken
                            )
                            storedFileId
                        } catch (error: Exception) {
                            if (error.message?.contains("HTTP 404") == true) {
                                preferences.edit()
                                    .remove(FILE_ID_KEY)
                                    .apply()

                                uploadNewFile(
                                    snapshot,
                                    accessToken
                                )
                            } else {
                                throw error
                            }
                        }
                    }

                preferences.edit()
                    .putString(
                        FILE_ID_KEY,
                        fileId
                    )
                    .apply()

                mainHandler.post {
                    callback(
                        Result.success(Unit)
                    )
                }
            } catch (error: Exception) {
                mainHandler.post {
                    callback(
                        Result.failure(error)
                    )
                }
            } finally {
                snapshot?.delete()
            }
        }.start()
    }

    private fun createDatabaseSnapshot(
        database: SupportSQLiteDatabase
    ): File {
        val snapshot =
            File(
                context.cacheDir,
                "ledger_backup_snapshot.db"
            )

        if (snapshot.exists()) {
            snapshot.delete()
        }

        val escapedPath =
            snapshot.absolutePath.replace(
                "'",
                "''"
            )

        database.execSQL(
            "VACUUM INTO '$escapedPath'"
        )

        if (!snapshot.exists() || snapshot.length() == 0L) {
            throw IllegalStateException(
                "Database snapshot could not be created."
            )
        }

        return snapshot
    }

    private fun uploadNewFile(
        snapshot: File,
        accessToken: String
    ): String {
        val boundary =
            "LedgerBackupBoundary${System.currentTimeMillis()}"

        val metadata =
            """
            {
              "name": "$BACKUP_FILE_NAME",
              "mimeType": "$MIME_TYPE",
              "parents": ["appDataFolder"]
            }
            """.trimIndent()

        val connection =
            URL(
                "$DRIVE_UPLOAD_URL?uploadType=multipart"
            ).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 60_000
            connection.readTimeout = 60_000

            connection.setRequestProperty(
                "Authorization",
                "Bearer $accessToken"
            )

            connection.setRequestProperty(
                "Content-Type",
                "multipart/related; boundary=$boundary"
            )

            connection.outputStream.use { output ->

                output.write(
                    "--$boundary\r\n".toByteArray()
                )

                output.write(
                    "Content-Type: application/json; charset=UTF-8\r\n\r\n"
                        .toByteArray()
                )

                output.write(
                    metadata.toByteArray()
                )

                output.write(
                    "\r\n--$boundary\r\n".toByteArray()
                )

                output.write(
                    "Content-Type: $MIME_TYPE\r\n\r\n"
                        .toByteArray()
                )

                snapshot.inputStream().use { input ->
                    val buffer =
                        ByteArray(16 * 1024)

                    while (true) {
                        val count =
                            input.read(buffer)

                        if (count <= 0) {
                            break
                        }

                        output.write(
                            buffer,
                            0,
                            count
                        )
                    }
                }

                output.write(
                    "\r\n--$boundary--\r\n".toByteArray()
                )
            }

            val responseCode =
                connection.responseCode

            if (responseCode !in 200..299) {
                val errorBody = readError(connection)

                Log.e(
                    "GoogleDriveBackup",
                    "UPLOAD HTTP $responseCode: $errorBody"
                )

                throw IOExceptionWithResponse(
                    responseCode,
                    errorBody
                )
            }

            val response =
                connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

            return JSONObject(response)
                .getString("id")
        } finally {
            connection.disconnect()
        }
    }

    private fun updateExistingFile(
        fileId: String,
        snapshot: File,
        accessToken: String
    ) {
        val encodedFileId =
            URLEncoder.encode(
                fileId,
                "UTF-8"
            )

        val connection =
            URL(
                "$DRIVE_UPLOAD_URL/$encodedFileId?uploadType=media"
            ).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "PATCH"
            connection.doOutput = true
            connection.connectTimeout = 60_000
            connection.readTimeout = 60_000

            connection.setRequestProperty(
                "Authorization",
                "Bearer $accessToken"
            )

            connection.setRequestProperty(
                "Content-Type",
                MIME_TYPE
            )

            snapshot.inputStream().use { input ->
                connection.outputStream.use { output ->
                    val buffer =
                        ByteArray(16 * 1024)

                    while (true) {
                        val count =
                            input.read(buffer)

                        if (count <= 0) {
                            break
                        }

                        output.write(
                            buffer,
                            0,
                            count
                        )
                    }
                }
            }

            val responseCode =
                connection.responseCode

            if (responseCode !in 200..299) {
                val errorBody = readError(connection)

                Log.e(
                    "GoogleDriveBackup",
                    "UPLOAD HTTP $responseCode: $errorBody"
                )

                throw IOExceptionWithResponse(
                    responseCode,
                    errorBody
                )
            }
        } finally {
            connection.disconnect()
        }
    }


    /**
     * Permanently deletes Ledger backup files from this app's Google Drive appDataFolder.
     * The callback receives failure if listing or any deletion fails.
     */
    fun deleteAllBackups(accessToken: String, callback: (Result<Unit>) -> Unit) {
        Thread {
            try {
                var pageToken: String? = null
                val fileIds = mutableListOf<String>()

                do {
                    val query = "name = 'Ledger Backup.db' and trashed = false and 'appDataFolder' in parents"
                    val url = buildString {
                        append(DRIVE_FILE_URL)
                        append("?spaces=appDataFolder&pageSize=100")
                        append("&fields=nextPageToken,files(id,name)")
                        append("&q=")
                        append(java.net.URLEncoder.encode(query, "UTF-8"))
                        if (!pageToken.isNullOrBlank()) {
                            append("&pageToken=")
                            append(java.net.URLEncoder.encode(pageToken, "UTF-8"))
                        }
                    }

                    val connection = java.net.URL(url).openConnection()
                        as java.net.HttpURLConnection
                    try {
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 20_000
                        connection.readTimeout = 30_000
                        connection.setRequestProperty("Authorization", "Bearer $accessToken")

                        val code = connection.responseCode
                        if (code !in 200..299) {
                            val body = connection.errorStream?.bufferedReader()
                                ?.use { it.readText() }.orEmpty()
                            throw java.io.IOException(
                                "Could not list Ledger Drive backups (HTTP $code): $body"
                            )
                        }

                        val json = org.json.JSONObject(
                            connection.inputStream.bufferedReader().use { it.readText() }
                        )
                        val files = json.optJSONArray("files")
                        if (files != null) {
                            for (i in 0 until files.length()) {
                                val file = files.optJSONObject(i) ?: continue
                                val id = file.optString("id").trim()
                                val name = file.optString("name")
                                if (id.isNotBlank() && name == "Ledger Backup.db") {
                                    fileIds.add(id)
                                }
                            }
                        }
                        pageToken = json.optString("nextPageToken")
                            .takeIf { it.isNotBlank() && it != "null" }
                    } finally {
                        connection.disconnect()
                    }
                } while (pageToken != null)

                for (fileId in fileIds.distinct()) {
                    val encodedId = java.net.URLEncoder.encode(fileId, "UTF-8")
                    val connection = java.net.URL("$DRIVE_FILE_URL/$encodedId")
                        .openConnection() as java.net.HttpURLConnection
                    try {
                        connection.requestMethod = "DELETE"
                        connection.connectTimeout = 20_000
                        connection.readTimeout = 30_000
                        connection.setRequestProperty("Authorization", "Bearer $accessToken")

                        val code = connection.responseCode
                        // 404 means the file has already been deleted.
                        if (code !in 200..299 && code != 404) {
                            val body = connection.errorStream?.bufferedReader()
                                ?.use { it.readText() }.orEmpty()
                            throw java.io.IOException(
                                "Could not delete Ledger Drive backup (HTTP $code): $body"
                            )
                        }
                    } finally {
                        connection.disconnect()
                    }
                }

                callback(Result.success(Unit))
            } catch (error: Exception) {
                callback(Result.failure(error))
            }
        }.start()
    }

    private fun readError(
        connection: HttpURLConnection
    ): String {
        return try {
            connection.errorStream
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: "No error response."
        } catch (_: Exception) {
            "Unable to read error response."
        }
    }

    private class IOExceptionWithResponse(
        code: Int,
        message: String
    ) : Exception(
        "Google Drive HTTP $code: $message"
    )
}

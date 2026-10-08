package com.ledger.app.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

class GoogleDriveRestore(
    context: Context
) {

    data class BackupMetadata(
        val fileId: String,
        val modifiedTime: String
    )


    companion object {
        private const val PREFERENCES_NAME = "ledger_backup"
        private const val FILE_ID_KEY = "google_drive_file_id"
        private const val DRIVE_FILE_URL =
            "https://www.googleapis.com/drive/v3/files"
    }

    private val context = context.applicationContext

    private val preferences =
        this.context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    private val mainHandler =
        Handler(Looper.getMainLooper())

    fun restore(
        accessToken: String,
        callback: (Result<Unit>) -> Unit
    ) {
        restore(accessToken, null, callback)
    }

    fun restore(
        accessToken: String,
        fileIdOverride: String?,
        callback: (Result<Unit>) -> Unit
    ) {
        Thread {
            val downloadedFile =
                File(
                    context.cacheDir,
                    "ledger_restore_download.db"
                )

            val validatedFile =
                File(
                    context.cacheDir,
                    "ledger_restore_validated.db"
                )

            try {
                val fileId =
                    fileIdOverride
                        ?: preferences.getString(
                            FILE_ID_KEY,
                            null
                        )

                if (fileId.isNullOrBlank()) {
                    throw NoBackupFoundException()
                }

                preferences.edit()
                    .putString(
                        FILE_ID_KEY,
                        fileId
                    )
                    .apply()

                downloadedFile.delete()
                validatedFile.delete()

                downloadBackup(
                    fileId,
                    accessToken,
                    downloadedFile
                )

                validateDatabase(
                    downloadedFile
                )

                downloadedFile.copyTo(
                    validatedFile,
                    overwrite = true
                )

                mainHandler.post {
                    callback(
                        Result.success(Unit)
                    )
                }
            } catch (error: Exception) {
                downloadedFile.delete()
                validatedFile.delete()

                mainHandler.post {
                    callback(
                        Result.failure(error)
                    )
                }
            }
        }.start()
    }

    fun findBackupFile(
        accessToken: String,
        callback: (Result<String?>) -> Unit
    ) {
        Thread {
            try {
                val metadata = findBackupMetadataBlocking(accessToken)

                mainHandler.post {
                    callback(
                        Result.success(metadata?.fileId)
                    )
                }
            } catch (error: Exception) {
                mainHandler.post {
                    callback(
                        Result.failure(error)
                    )
                }
            }
        }.start()
    }

    fun findBackupMetadata(
        accessToken: String,
        callback: (Result<BackupMetadata?>) -> Unit
    ) {
        Thread {
            try {
                val metadata = findBackupMetadataBlocking(accessToken)

                mainHandler.post {
                    callback(
                        Result.success(metadata)
                    )
                }
            } catch (error: Exception) {
                mainHandler.post {
                    callback(
                        Result.failure(error)
                    )
                }
            }
        }.start()
    }

    private fun findBackupMetadataBlocking(
        accessToken: String
    ): BackupMetadata? {
        val encodedQuery =
            URLEncoder.encode(
                "name = 'Ledger Backup.db' and trashed = false",
                "UTF-8"
            )

        val encodedFields =
            URLEncoder.encode(
                "files(id,name,mimeType,modifiedTime)",
                "UTF-8"
            )

        val url =
            URL(
                "https://www.googleapis.com/drive/v3/files" +
                    "?q=$encodedQuery" +
                    "&spaces=drive" +
                    "&pageSize=10" +
                    "&fields=$encodedFields"
            )

        val connection =
            url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 60_000
            connection.readTimeout = 60_000

            connection.setRequestProperty(
                "Authorization",
                "Bearer $accessToken"
            )

            val responseCode =
                connection.responseCode

            if (responseCode !in 200..299) {
                val errorBody =
                    connection.errorStream
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: "No error response."

                throw Exception(
                    "Google Drive HTTP $responseCode: $errorBody"
                )
            }

            val response =
                connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

            val json =
                org.json.JSONObject(response)

            val files =
                json.optJSONArray("files")
                    ?: return null

            if (files.length() == 0) {
                return null
            }

            val file = files.getJSONObject(0)

            val fileId = file.getString("id")
            val modifiedTime = file.optString("modifiedTime")

            if (modifiedTime.isBlank()) {
                throw IllegalStateException(
                    "Google Drive backup modifiedTime is unavailable."
                )
            }

            return BackupMetadata(
                fileId = fileId,
                modifiedTime = modifiedTime
            )
        } finally {
            connection.disconnect()
        }
    }

    class NoBackupFoundException : Exception(
        "No Ledger backup found on Google Drive."
    )

    private fun downloadBackup(
        fileId: String,
        accessToken: String,
        destination: File
    ) {
        val encodedFileId =
            URLEncoder.encode(
                fileId,
                "UTF-8"
            )

        val connection =
            URL(
                "$DRIVE_FILE_URL/$encodedFileId?alt=media"
            ).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 60_000
            connection.readTimeout = 60_000

            connection.setRequestProperty(
                "Authorization",
                "Bearer $accessToken"
            )

            val responseCode =
                connection.responseCode

            if (responseCode !in 200..299) {
                val errorBody =
                    connection.errorStream
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: "No error response."

                throw Exception(
                    "Google Drive HTTP $responseCode: $errorBody"
                )
            }

            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
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

            if (
                !destination.exists() ||
                destination.length() == 0L
            ) {
                throw IllegalStateException(
                    "Downloaded backup is empty."
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun validateDatabase(
        file: File
    ) {
        val database =
            SQLiteDatabase.openDatabase(
                file.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

        try {
            database.rawQuery(
                "PRAGMA integrity_check",
                null
            ).use { cursor ->
                if (!cursor.moveToFirst()) {
                    throw IllegalStateException(
                        "Database integrity check returned no result."
                    )
                }

                val result =
                    cursor.getString(0)

                if (result != "ok") {
                    throw IllegalStateException(
                        "Backup database failed integrity check: $result"
                    )
                }
            }

            database.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='parties'",
                null
            ).use { cursor ->
                if (!cursor.moveToFirst()) {
                    throw IllegalStateException(
                        "Backup is not a valid Ledger database."
                    )
                }
            }

            database.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='ledger_entries'",
                null
            ).use { cursor ->
                if (!cursor.moveToFirst()) {
                    throw IllegalStateException(
                        "Backup is missing ledger_entries table."
                    )
                }
            }
        } finally {
            database.close()
        }
    }

    fun getValidatedBackupFile(): File {
        val file =
            File(
                context.cacheDir,
                "ledger_restore_validated.db"
            )

        if (!file.exists()) {
            throw IllegalStateException(
                "Validated backup file does not exist."
            )
        }

        return file
    }
}

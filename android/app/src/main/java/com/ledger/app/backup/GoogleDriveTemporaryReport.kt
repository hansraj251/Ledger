package com.ledger.app.backup

import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Uploads one temporary party PDF. It never uses or modifies the database
 * backup file. The public Drive permission is removed in effect when the
 * file itself is deleted by the scheduled cleanup script.
 */
class GoogleDriveTemporaryReport {

    companion object {
        private const val MIME_TYPE = "application/pdf"
        private const val MARKER_KEY = "alignledgerTempReport"
        private const val MARKER_VALUE = "true"
        private const val DRIVE_UPLOAD_URL =
            "https://www.googleapis.com/upload/drive/v3/files"
        private const val DRIVE_FILES_URL =
            "https://www.googleapis.com/drive/v3/files"
        private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    }

    fun uploadAndShare(
        source: File,
        partyName: String,
        accessToken: String
    ): String {
        require(source.isFile && source.length() > 0L) {
            "Party report PDF is missing or empty."
        }
        require(accessToken.isNotBlank()) { "Google Drive authorization is missing." }

        val now = System.currentTimeMillis()
        val expiresAt = now + DAY_MILLIS
        val safeName = partyName.trim()
            .replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .take(70)
            .ifBlank { "party" }
        val fileName = "${safeName}_ledger_report_$now.pdf"
        val boundary = "AlignLedgerReportBoundary$now"

        val metadata = JSONObject()
            .put("name", fileName)
            .put("mimeType", MIME_TYPE)
            .put(
                "description",
                "ALIGNLEDGER_TEMP_REPORT; expiresAt=$expiresAt"
            )
            .put(
                "properties",
                JSONObject()
                    .put(MARKER_KEY, MARKER_VALUE)
                    .put("expiresAt", expiresAt.toString())
            )
            .toString()

        val upload = URL(
            "$DRIVE_UPLOAD_URL?uploadType=multipart&fields=id"
        ).openConnection() as HttpURLConnection

        val fileId: String
        try {
            upload.requestMethod = "POST"
            upload.doOutput = true
            upload.connectTimeout = 60_000
            upload.readTimeout = 60_000
            upload.setRequestProperty("Authorization", "Bearer $accessToken")
            upload.setRequestProperty(
                "Content-Type",
                "multipart/related; boundary=$boundary"
            )

            upload.outputStream.use { out ->
                out.write("--$boundary\r\n".toByteArray())
                out.write(
                    "Content-Type: application/json; charset=UTF-8\r\n\r\n"
                        .toByteArray()
                )
                out.write(metadata.toByteArray(Charsets.UTF_8))
                out.write("\r\n--$boundary\r\n".toByteArray())
                out.write("Content-Type: $MIME_TYPE\r\n\r\n".toByteArray())
                source.inputStream().use { it.copyTo(out) }
                out.write("\r\n--$boundary--\r\n".toByteArray())
            }

            val response = readResponse(upload)
            if (upload.responseCode !in 200..299) {
                error("Google Drive upload failed (HTTP ${upload.responseCode}): $response")
            }
            fileId = JSONObject(response).getString("id")
        } finally {
            upload.disconnect()
        }

        try {
            createPublicReadPermission(fileId, accessToken)
        } catch (error: Exception) {
            // Do not leave an uploaded report behind if sharing failed.
            runCatching { deleteFile(fileId, accessToken) }
            throw error
        }

        return "https://drive.google.com/file/d/$fileId/view"
    }

    private fun createPublicReadPermission(fileId: String, accessToken: String) {
        val encodedId = URLEncoder.encode(fileId, "UTF-8")
        val connection = URL(
            "$DRIVE_FILES_URL/$encodedId/permissions?fields=id"
        ).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use {
                it.write(
                    JSONObject()
                        .put("type", "anyone")
                        .put("role", "reader")
                        .toString()
                        .toByteArray(Charsets.UTF_8)
                )
            }

            val response = readResponse(connection)
            if (connection.responseCode !in 200..299) {
                error(
                    "Could not enable link access (HTTP ${connection.responseCode}): $response"
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun deleteFile(fileId: String, accessToken: String) {
        val encodedId = URLEncoder.encode(fileId, "UTF-8")
        val connection = URL("$DRIVE_FILES_URL/$encodedId")
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "DELETE"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            val response = readResponse(connection)
            if (connection.responseCode !in 200..299) {
                error("Drive cleanup after sharing failure failed: $response")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun readResponse(connection: HttpURLConnection): String {
        return try {
            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }
}

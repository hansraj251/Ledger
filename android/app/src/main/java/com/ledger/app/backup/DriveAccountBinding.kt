package com.ledger.app.backup

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.Executors

/** Verifies the OAuth token's Google identity before changing the active Drive binding. */
object DriveAccountBinding {
    private const val PREFS = "ledger_drive_account_binding"
    private const val KEY_EMAIL = "drive_account_email"
    private val executor = Executors.newSingleThreadExecutor()

    fun currentAccountId(context: Context): String? = currentEmail(context)?.let(::accountIdForEmail)

    fun currentEmail(context: Context): String? = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY_EMAIL, null)
        ?.trim()
        ?.lowercase(Locale.ROOT)
        ?.takeIf { it.isNotBlank() && it.contains("@") }

    fun accountIdForEmail(email: String): String = sha256(email.trim().lowercase(Locale.ROOT))

    fun verifyAndCapture(
        context: Context,
        accessToken: String?,
        onComplete: (Result<String>) -> Unit
    ) {
        val appContext = context.applicationContext
        if (accessToken.isNullOrBlank()) {
            onComplete(Result.failure(IllegalArgumentException("Google Drive access token is unavailable.")))
            return
        }
        executor.execute {
            val result = runCatching {
                val connection = URL("https://www.googleapis.com/oauth2/v2/userinfo")
                    .openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.setRequestProperty("Authorization", "Bearer $accessToken")
                    if (connection.responseCode !in 200..299) {
                        throw IllegalStateException("Google account identity request failed: HTTP ${connection.responseCode}")
                    }
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val email = JSONObject(body).optString("email")
                        .trim().lowercase(Locale.ROOT)
                    if (email.isBlank() || !email.contains("@")) {
                        throw IllegalStateException("Google did not return a valid account email.")
                    }
                    val saved = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit().putString(KEY_EMAIL, email).commit()
                    if (!saved) throw IllegalStateException("Google Drive account identity could not be saved.")
                    accountIdForEmail(email)
                } finally {
                    connection.disconnect()
                }
            }
            result.onFailure { Log.w("DriveAccountBinding", "Unable to verify Google Drive account identity", it) }
            onComplete(result)
        }
    }

    /** Backward-compatible wrapper. Failed verification never erases the last verified identity. */
    fun capture(context: Context, accessToken: String?, onComplete: (() -> Unit)? = null) {
        verifyAndCapture(context, accessToken) { onComplete?.invoke() }
    }

    fun isOwnerPremiumAccount(context: Context): Boolean = false

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

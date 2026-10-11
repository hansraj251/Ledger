package com.ledger.app.backup

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Identifies the Google account represented by a Google Drive OAuth access token.
 * The email is stored locally; Billing receives only its SHA-256 account identifier.
 */
object DriveAccountBinding {
    private const val PREFS = "ledger_drive_account_binding"
    private const val KEY_EMAIL = "drive_account_email"
    private const val OWNER_PREMIUM_EMAIL = "hans004333@gmail.com"
    private val executor = Executors.newSingleThreadExecutor()

    fun currentAccountId(context: Context): String? {
        val email = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_EMAIL, null)
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?: return null

        if (email.isBlank()) return null
        return sha256(email)
    }

    fun isOwnerPremiumAccount(context: Context): Boolean {
        val email = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_EMAIL, null)
            ?.trim()
            ?.lowercase(Locale.ROOT)

        return email == OWNER_PREMIUM_EMAIL
    }

    fun capture(
        context: Context,
        accessToken: String?,
        onComplete: (() -> Unit)? = null
    ) {
        if (accessToken.isNullOrBlank()) {
            context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_EMAIL)
                .apply()
            onComplete?.invoke()
            return
        }

        val appContext = context.applicationContext
        executor.execute {
            try {
                val connection = URL(
                    "https://www.googleapis.com/oauth2/v2/userinfo"
                ).openConnection() as HttpURLConnection

                try {
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer $accessToken"
                    )

                    if (connection.responseCode !in 200..299) {
                        throw IllegalStateException(
                            "Google account identity request failed: HTTP ${connection.responseCode}"
                        )
                    }

                    val body = connection.inputStream.bufferedReader().use {
                        it.readText()
                    }
                    val email = JSONObject(body)
                        .optString("email")
                        .trim()
                        .lowercase(Locale.ROOT)

                    if (email.isBlank() || !email.contains("@")) {
                        throw IllegalStateException(
                            "Google did not return a valid account email."
                        )
                    }

                    appContext.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                    ).edit()
                        .putString(KEY_EMAIL, email)
                        .apply()
                } finally {
                    connection.disconnect()
                }
            } catch (error: Exception) {
                appContext.getSharedPreferences(
                    PREFS,
                    Context.MODE_PRIVATE
                ).edit()
                    .remove(KEY_EMAIL)
                    .apply()
                Log.w(
                    "DriveAccountBinding",
                    "Unable to capture Google Drive account identity",
                    error
                )
            } finally {
                onComplete?.invoke()
            }
        }
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

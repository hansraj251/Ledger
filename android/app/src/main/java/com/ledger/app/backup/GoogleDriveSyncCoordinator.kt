package com.ledger.app.backup

import android.content.Context
import android.content.IntentSender
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.atomic.AtomicLong

class GoogleDriveSyncCoordinator(
    context: Context,
    private val authorization: GoogleDriveAuthorization,
    private val backup: GoogleDriveBackup,
    private val database: SupportSQLiteDatabase,
    private val onAuthorizationRequired: ((IntentSender) -> Unit)? = null,
    private val onSyncStarted: (() -> Unit)? = null,
    private val onSyncSuccess: (() -> Unit)? = null,
    private val onSyncFailure: ((Throwable) -> Unit)? = null
) {
    companion object {
        private const val PREFS_NAME = "ledger_google_drive_sync"
        private const val PENDING_GENERATION = "pending_generation"
        private const val COMPLETED_GENERATION = "completed_generation"
    }

    private val prefs =
        context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    private val lock = Any()

    private val generation =
        AtomicLong(
            prefs.getLong(
                PENDING_GENERATION,
                0L
            )
        )

    private var completedGeneration =
        prefs.getLong(
            COMPLETED_GENERATION,
            0L
        )

    private var running = false
    private var paused = false

    fun requestAutoSync() {
        requestSync(manual = false)
    }

    fun requestManualSync() {
        requestSync(manual = true)
    }

    fun pause() {
        synchronized(lock) {
            paused = true
        }
    }

    fun resume() {
        val shouldStart = synchronized(lock) {
            paused = false

            if (
                !running &&
                generation.get() > completedGeneration
            ) {
                running = true
                true
            } else {
                false
            }
        }

        if (shouldStart) {
            startSync(manual = false)
        }
    }

    private fun requestSync(manual: Boolean) {
        val shouldStart = synchronized(lock) {
            generation.incrementAndGet()
            persistPendingGeneration()

            if (
                !paused &&
                !running
            ) {
                running = true
                true
            } else {
                false
            }
        }

        if (shouldStart) {
            startSync(manual)
        }
    }

    private fun startSync(manual: Boolean) {
        val syncGeneration = generation.get()

        onSyncStarted?.invoke()

        authorization
            .authorize()
            .addOnSuccessListener { result ->

                if (result.hasResolution()) {
                    synchronized(lock) {
                        running = false
                    }

                    if (manual) {
                        val pendingIntent = result.pendingIntent

                        if (pendingIntent != null) {
                            onAuthorizationRequired?.invoke(
                                pendingIntent.intentSender
                            )
                        } else {
                            onSyncFailure?.invoke(
                                IllegalStateException(
                                    "Google Drive authorization is required."
                                )
                            )
                        }
                    }

                    return@addOnSuccessListener
                }

                val accessToken = result.accessToken

                if (accessToken.isNullOrBlank()) {
                    finishSync(
                        syncGeneration,
                        success = false,
                        error = IllegalStateException(
                            "Google Drive access token is unavailable."
                        )
                    )
                    return@addOnSuccessListener
                }

                backup.sync(
                    database,
                    accessToken
                ) { syncResult ->

                    syncResult
                        .onSuccess {
                            finishSync(
                                syncGeneration,
                                success = true,
                                error = null
                            )
                        }
                        .onFailure { error ->
                            finishSync(
                                syncGeneration,
                                success = false,
                                error = error
                            )
                        }
                }
            }
            .addOnFailureListener { error ->
                finishSync(
                    syncGeneration,
                    success = false,
                    error = error
                )
            }
    }

    private fun finishSync(
        syncGeneration: Long,
        success: Boolean,
        error: Throwable?
    ) {
        var startNext = false

        synchronized(lock) {
            running = false

            if (success) {
                if (generation.get() == syncGeneration) {
                    completedGeneration = syncGeneration
                    persistCompletedGeneration()
                } else if (!paused) {
                    /*
                     * Changes happened while this upload was running.
                     * Do not start another upload in parallel; start it
                     * only after this upload has completely finished.
                     */
                    running = true
                    startNext = true
                }
            }
        }

        if (!success) {
            error?.let {
                onSyncFailure?.invoke(it)
            }
            return
        }

        onSyncSuccess?.invoke()

        if (startNext) {
            startSync(manual = false)
        }
    }

    private fun persistPendingGeneration() {
        prefs.edit()
            .putLong(
                PENDING_GENERATION,
                generation.get()
            )
            .apply()
    }

    private fun persistCompletedGeneration() {
        prefs.edit()
            .putLong(
                COMPLETED_GENERATION,
                completedGeneration
            )
            .apply()
    }
}

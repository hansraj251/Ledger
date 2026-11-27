package com.ledger.app.backup

import android.content.Context
import android.content.IntentSender
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

/** Serializes Drive sync and keeps all sync state isolated by verified Google account. */
class GoogleDriveSyncCoordinator(
    context: Context,
    private val authorization: GoogleDriveAuthorization,
    private val backup: GoogleDriveBackup,
    private val database: SupportSQLiteDatabase,
    private val onAuthorizationRequired: ((IntentSender) -> Unit)? = null,
    private val onSyncStarted: (() -> Unit)? = null,
    private val onSyncSuccess: (() -> Unit)? = null,
    private val onSyncFailure: ((Throwable) -> Unit)? = null,
    private val onRemoteBackupNewer: ((String, String, String, String) -> Unit)? = null
) {
    companion object {
        private const val PREFS_NAME = "ledger_google_drive_sync"
        private const val PENDING_GENERATION = "pending_generation_"
        private const val COMPLETED_GENERATION = "completed_generation_"
        private const val LAST_SYNCED_DRIVE_MODIFIED_TIME = "last_synced_drive_modified_time_"
        private const val REMOTE_DECISION_PENDING = "remote_decision_pending_"
    }

    private val applicationContext = context.applicationContext
    private val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val lock = Any()
    private val generation = AtomicLong(0L)
    private var completedGeneration = 0L
    private var activeAccountId: String? = null
    private var running = false
    private var paused = false

    private fun pendingKey(id: String) = PENDING_GENERATION + id
    private fun completedKey(id: String) = COMPLETED_GENERATION + id
    private fun modifiedKey(id: String) = LAST_SYNCED_DRIVE_MODIFIED_TIME + id
    private fun remoteDecisionKey(id: String) = REMOTE_DECISION_PENDING + id

    private fun activateAccount(accountId: String) {
        synchronized(lock) {
            if (activeAccountId == accountId) return
            // Legacy global sync values are intentionally ignored: their owning Gmail
            // cannot be proven after an account switch. Each account starts with isolated state.
            activeAccountId = accountId
            generation.set(prefs.getLong(pendingKey(accountId), 0L))
            completedGeneration = prefs.getLong(completedKey(accountId), 0L)
            running = false
        }
    }

    fun bindVerifiedAccount(accountId: String) {
        require(accountId.matches(Regex("[a-f0-9]{64}")))
        activateAccount(accountId)
    }

    fun requestAutoSync() {
        android.util.Log.d("LedgerSyncDebug", "requestAutoSync() called")
        requestSync(manual = false)
    }

    fun requestManualSync() = requestSync(manual = true)

    fun pause() { synchronized(lock) { paused = true } }

    fun resume() {
        synchronized(lock) { if (running) return; paused = true; running = true }
        authorization.authorize()
            .addOnSuccessListener { auth ->
                if (auth.hasResolution()) {
                    requestAuthorization(auth.pendingIntent?.intentSender)
                    return@addOnSuccessListener
                }
                val token = auth.accessToken
                DriveAccountBinding.verifyAndCapture(applicationContext, token) { verified ->
                    verified.onSuccess { accountId ->
                        activateAccount(accountId)
                        checkRemoteBackupBeforeSync(token!!, accountId)
                    }.onFailure {
                        finishStartupCheck(false)
                        onSyncFailure?.invoke(it)
                    }
                }
            }
            .addOnFailureListener {
                finishStartupCheck(false)
                onSyncFailure?.invoke(it)
            }
    }

    private fun requestAuthorization(sender: IntentSender?) {
        if (sender == null) {
            finishStartupCheck(false)
            onSyncFailure?.invoke(IllegalStateException("Google Drive authorization is required."))
        } else {
            try { onAuthorizationRequired?.invoke(sender) }
            catch (error: Exception) { onAuthorizationCancelled(); onSyncFailure?.invoke(error) }
            synchronized(lock) { running = false }
        }
    }

    fun onAuthorizationCancelled() { synchronized(lock) { running = false; paused = false } }

    private fun checkRemoteBackupBeforeSync(accessToken: String, accountId: String) {
        GoogleDriveRestore(applicationContext).findBackupMetadata(accessToken) { result ->
            result.onSuccess { metadata ->
                if (synchronized(lock) { activeAccountId != accountId }) return@onSuccess
                if (metadata == null) {
                    // No remote backup: retain local data. Only upload if this account has pending changes.
                    finishStartupCheck(startPendingSync = true)
                    return@onSuccess
                }
                val remote = runCatching { Instant.parse(metadata.modifiedTime) }.getOrNull()
                if (remote == null) {
                    prefs.edit().putBoolean(remoteDecisionKey(accountId), true).commit()
                    synchronized(lock) { paused = true; running = false }
                    onSyncFailure?.invoke(
                        IllegalStateException("Remote backup time could not be verified. Sync is paused to protect your data.")
                    )
                    return@onSuccess
                }
                val local = prefs.getString(modifiedKey(accountId), null)
                    ?.let { runCatching { Instant.parse(it) }.getOrNull() }
                val hasPending = synchronized(lock) { generation.get() > completedGeneration }
                if (local == null || remote.isAfter(local)) {
                    // Ask the user before pending local changes can upload over an
                    // unseen or newer remote backup. Persist the choice so a later
                    // autosync cannot bypass the prompt after it is dismissed.
                    val savedDecision = prefs.edit()
                        .putBoolean(remoteDecisionKey(accountId), true)
                        .commit()
                    synchronized(lock) { paused = true; running = false }
                    if (savedDecision) {
                        onRemoteBackupNewer?.invoke(metadata.fileId, metadata.modifiedTime, accessToken, accountId)
                    } else {
                        onSyncFailure?.invoke(
                            IllegalStateException("Backup choice could not be saved. Sync is paused to protect your data.")
                        )
                    }
                } else if (hasPending) {
                    prefs.edit().putBoolean(remoteDecisionKey(accountId), false).commit()
                    finishStartupCheck(true)
                } else {
                    prefs.edit().putBoolean(remoteDecisionKey(accountId), false).commit()
                    finishStartupCheck(false)
                }
            }.onFailure {
                // Ignore a stale callback if the user has switched Drive accounts meanwhile.
                if (synchronized(lock) { activeAccountId != accountId }) return@onFailure
                // A failed remote check must block direct autosync until the remote
                // state can be checked again; otherwise a later request could overwrite
                // a backup that was simply unavailable during this check.
                prefs.edit().putBoolean(remoteDecisionKey(accountId), true).commit()
                synchronized(lock) { paused = true; running = false }
                onSyncFailure?.invoke(it)
            }
        }
    }

    private fun finishStartupCheck(startPendingSync: Boolean) {
        val shouldStart = synchronized(lock) {
            paused = false
            running = false
            val pending = startPendingSync && generation.get() > completedGeneration
            if (pending) running = true
            pending
        }
        if (shouldStart) startSync(manual = false)
    }

    fun markRemoteRestoreCompleted(modifiedTime: String) = markRemoteBackupSynced(modifiedTime)

    fun markNoRemoteBackupFound() {
        val id = activeAccountId ?: return
        synchronized(lock) {
            val current = generation.get()
            completedGeneration = current
            prefs.edit().putLong(pendingKey(id), current).putLong(completedKey(id), current)
                .remove(modifiedKey(id)).putBoolean(remoteDecisionKey(id), false).commit()
            paused = false
            running = false
        }
    }

    fun markRemoteBackupSynced(modifiedTime: String) {
        val id = activeAccountId ?: return
        synchronized(lock) {
            val current = generation.get()
            completedGeneration = current
            prefs.edit().putString(modifiedKey(id), modifiedTime)
                .putLong(pendingKey(id), current).putLong(completedKey(id), current)
                .putBoolean(remoteDecisionKey(id), false).commit()
            paused = false
            running = false
        }
    }

    /**
     * Called only after the user explicitly chooses "Sync Local Data" when a
     * different/newer remote backup exists. This bypasses the remote conflict
     * check exactly once and uploads the current local database to the verified
     * active Drive account.
     */
    fun syncLocalDataAfterRemoteConflict() {
        val accountId = synchronized(lock) { activeAccountId }
        if (accountId.isNullOrBlank() ||
            DriveAccountBinding.currentAccountId(applicationContext) != accountId
        ) {
            onSyncFailure?.invoke(
                IllegalStateException("Google Drive account changed. Local data was not synced.")
            )
            return
        }

        val shouldStart = synchronized(lock) {
            if (activeAccountId != accountId) {
                false
            } else {
                val nextGeneration = generation.incrementAndGet()
                val saved = prefs.edit()
                    .putLong(pendingKey(accountId), nextGeneration)
                    .putBoolean(remoteDecisionKey(accountId), false)
                    .commit()
                if (saved) {
                    paused = false
                    running = true
                    true
                } else {
                    paused = true
                    running = false
                    false
                }
            }
        }

        if (shouldStart) {
            startSync(manual = false)
        } else {
            onSyncFailure?.invoke(
                IllegalStateException("Could not save the local sync request. No backup was uploaded.")
            )
        }
    }

    fun resumeAfterRemoteRestoreFailure() = finishStartupCheck(startPendingSync = false)

    private fun requestSync(manual: Boolean) {
        val id = DriveAccountBinding.currentAccountId(applicationContext)
        if (id.isNullOrBlank()) {
            onSyncFailure?.invoke(IllegalStateException("Verify Google Drive account before syncing."))
            return
        }
        activateAccount(id)

        var shouldRecheckRemote = false
        val shouldStart = synchronized(lock) {
            generation.incrementAndGet()
            val saved = prefs.edit().putLong(pendingKey(id), generation.get()).commit()
            if (!saved) {
                onSyncFailure?.invoke(IllegalStateException("Pending sync state could not be saved."))
                false
            } else if (prefs.getBoolean(remoteDecisionKey(id), false)) {
                // Autosync records local edits but cannot overwrite an unresolved
                // remote backup. The next sync attempt rechecks and reopens the choice.
                paused = true
                running = false
                shouldRecheckRemote = true
                false
            } else if (!paused && !running) {
                running = true
                true
            } else false
        }

        if (shouldRecheckRemote) {
            resume()
        } else if (shouldStart) {
            startSync(manual)
        }
    }

    private fun startSync(manual: Boolean) {
        val accountId = activeAccountId ?: run { synchronized(lock) { running = false }; return }
        val syncGeneration = generation.get()
        onSyncStarted?.invoke()
        authorization.authorize()
            .addOnSuccessListener { auth ->
                if (auth.hasResolution()) { requestAuthorization(auth.pendingIntent?.intentSender); return@addOnSuccessListener }
                val token = auth.accessToken
                DriveAccountBinding.verifyAndCapture(applicationContext, token) { verified ->
                    verified.onSuccess { verifiedId ->
                        if (verifiedId != accountId || DriveAccountBinding.currentAccountId(applicationContext) != accountId) {
                            finishSync(syncGeneration, accountId, null, IllegalStateException("Google Drive account changed during sync. No backup was uploaded."))
                            return@onSuccess
                        }
                        if (token.isNullOrBlank()) {
                            finishSync(syncGeneration, accountId, null, IllegalStateException("Google Drive access token is unavailable."))
                            return@onSuccess
                        }
                        backup.sync(database, token, accountId) { syncResult ->
                            syncResult.onSuccess { finishSync(syncGeneration, accountId, it, null) }
                                .onFailure { finishSync(syncGeneration, accountId, null, it) }
                        }
                    }.onFailure { finishSync(syncGeneration, accountId, null, it) }
                }
            }
            .addOnFailureListener { finishSync(syncGeneration, accountId, null, it) }
    }

    private fun finishSync(syncGeneration: Long, accountId: String, backupResult: GoogleDriveBackupResult?, error: Throwable?) {
        if (backupResult == null) {
            synchronized(lock) {
                if (activeAccountId == accountId) running = false
            }
            error?.let { onSyncFailure?.invoke(it) }
            return
        }
        var startNext = false
        val saved = prefs.edit().putString(modifiedKey(accountId), backupResult.modifiedTime)
            .putLong(completedKey(accountId), syncGeneration)
            .putBoolean(remoteDecisionKey(accountId), false).commit()
        synchronized(lock) {
            if (activeAccountId == accountId) {
                if (saved) {
                    completedGeneration = syncGeneration
                    if (generation.get() > syncGeneration && !paused) { running = true; startNext = true }
                    else running = false
                } else running = false
            }
        }
        if (!saved) { onSyncFailure?.invoke(IllegalStateException("Backup uploaded, but account-specific sync status could not be saved.")); return }
        onSyncSuccess?.invoke()
        if (startNext && activeAccountId == accountId) startSync(manual = false)
    }
}

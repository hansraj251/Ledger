package com.ledger.app.cloudgroup

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.CancellationException

data class CloudAuthenticatedUser(
    val userId: String,
    val email: String
)

class CloudAuthManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "CloudAuthManager"
        private const val WEB_GOOGLE_CLIENT_ID =
            "629226729228-lq42j5k9dp0a4jgvo13rsdtui93dohep.apps.googleusercontent.com"
    }

    private val supabase = CloudSupabaseClient.client
    private val cloudGroupRemote = CloudSupabaseRemoteDataSource(supabase)

    private suspend fun claimPendingMembershipsSafely() {
        try {
            val claimedCount = cloudGroupRemote.claimPendingMemberships()
            Log.i(TAG, "Pending cloud group memberships claimed: $claimedCount")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Could not claim pending cloud group memberships", error)
        }
    }
    private val credentialManager = CredentialManager.create(context)

    suspend fun signInWithGoogle(): CloudAuthenticatedUser {
        Log.i(TAG, "STEP 1: Google sign-in started")

        val rawNonce = UUID.randomUUID().toString()
        val hashedNonce = MessageDigest
            .getInstance("SHA-256")
            .digest(rawNonce.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }

        val googleOption = GetSignInWithGoogleOption.Builder(
            WEB_GOOGLE_CLIENT_ID
        )
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        Log.i(TAG, "STEP 2: Requesting Google credential")

        val result = credentialManager.getCredential(
            context = context,
            request = request
        )

        Log.i(TAG, "STEP 3: Google credential received")

        val credential = GoogleIdTokenCredential.createFrom(
            result.credential.data
        )

        Log.i(TAG, "STEP 4: Google ID token parsed; starting Supabase authentication")

        try {
            supabase.auth.signInWith(IDToken) {
                idToken = credential.idToken
                provider = Google
                nonce = rawNonce
            }
            Log.i(TAG, "STEP 5: Supabase authentication request completed")
        } catch (error: Exception) {
            Log.e(TAG, "STEP 5 FAILED: Supabase authentication error", error)
            throw error
        }

        val user = supabase.auth.currentUserOrNull()
            ?: error("Supabase login succeeded but user session was not found.")

        val email = user.email
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
            ?: error("Authenticated Google account did not provide an email.")

        Log.i(TAG, "STEP 6: Authenticated user session found")

        val displayName = credential.displayName
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        if (displayName != null) {
            try {
                val updatedCount =
                    cloudGroupRemote.syncMyCloudProfileName(displayName)

                Log.i(
                    TAG,
                    "Google display name synced. Owner records updated: $updatedCount"
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w(TAG, "Could not sync Google display name", error)
            }
        } else {
            Log.w(TAG, "Google credential did not provide a display name")
        }

        claimPendingMembershipsSafely()
        FcmTokenRepository.registerCurrentDevice()

        return CloudAuthenticatedUser(
            userId = user.id,
            email = email
        )
    }

    suspend fun restoreSession(): CloudAuthenticatedUser? {
        Log.i(TAG, "Restoring Supabase session")
        supabase.auth.loadFromStorage()

        val restoredUser = currentUser()
        if (restoredUser != null) {
            claimPendingMembershipsSafely()
            FcmTokenRepository.registerCurrentDevice()
        }

        return restoredUser
    }

    fun currentUser(): CloudAuthenticatedUser? {
        val user = supabase.auth.currentUserOrNull() ?: return null
        val email = user.email
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return CloudAuthenticatedUser(
            userId = user.id,
            email = email
        )
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }
}

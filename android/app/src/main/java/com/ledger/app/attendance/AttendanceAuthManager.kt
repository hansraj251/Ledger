package com.ledger.app.attendance

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import java.security.MessageDigest
import java.util.UUID

data class AttendanceAuthenticatedUser(
    val userId: String,
    val email: String
)

/**
 * Google authentication for the Attendance client only.
 *
 * This class deliberately does not call Online Groups profile or membership RPCs.
 */
class AttendanceAuthManager(context: Context) {

    companion object {
        private const val WEB_GOOGLE_CLIENT_ID =
            "629226729228-lq42j5k9dp0a4jgvo13rsdtui93dohep.apps.googleusercontent.com"
    }

    private val credentialContext = context
    private val credentialManager = CredentialManager.create(context)
    private val supabase = AttendanceSupabaseClient.client

    suspend fun signInWithGoogle(): AttendanceAuthenticatedUser {
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

        val result = credentialManager.getCredential(
            context = credentialContext,
            request = request
        )

        val credential = GoogleIdTokenCredential.createFrom(
            result.credential.data
        )

        supabase.auth.signInWith(IDToken) {
            idToken = credential.idToken
            provider = Google
            nonce = rawNonce
        }

        return currentUser()
            ?: error("Attendance sign-in completed but the user session was not found.")
    }

    suspend fun restoreSession(): AttendanceAuthenticatedUser? {
        supabase.auth.loadFromStorage()
        return currentUser()
    }

    fun currentUser(): AttendanceAuthenticatedUser? {
        val user = supabase.auth.currentUserOrNull() ?: return null
        val email = user.email
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return AttendanceAuthenticatedUser(
            userId = user.id,
            email = email
        )
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }

}

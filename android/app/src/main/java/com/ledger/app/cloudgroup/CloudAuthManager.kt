package com.ledger.app.cloudgroup

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import java.security.MessageDigest
import java.util.UUID

data class CloudAuthenticatedUser(
    val userId: String,
    val email: String
)

class CloudAuthManager(
    private val context: Context
) {
    companion object {
        private const val WEB_GOOGLE_CLIENT_ID =
            "629226729228-lq42j5k9dp0a4jgvo13rsdtui93dohep.apps.googleusercontent.com"
    }

    private val supabase = CloudSupabaseClient.client
    private val credentialManager = CredentialManager.create(context)

    suspend fun signInWithGoogle(): CloudAuthenticatedUser {
        val rawNonce = UUID.randomUUID().toString()

        val hashedNonce = MessageDigest
            .getInstance("SHA-256")
            .digest(rawNonce.toByteArray())
            .joinToString("") { byte ->
                "%02x".format(byte)
            }

        val googleOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_GOOGLE_CLIENT_ID)
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        val result = credentialManager.getCredential(
            context = context,
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

        val user = supabase.auth.currentUserOrNull()
            ?: error("Supabase login succeeded but user session was not found.")

        val email = user.email
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
            ?: error("Authenticated Google account did not provide an email.")

        return CloudAuthenticatedUser(
            userId = user.id,
            email = email
        )
    }

    fun currentUser(): CloudAuthenticatedUser? {
        val user = supabase.auth.currentUserOrNull() ?: return null
        val email = user.email?.trim()?.lowercase()
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

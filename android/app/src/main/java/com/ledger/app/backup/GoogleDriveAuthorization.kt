package com.ledger.app.backup

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope

class GoogleDriveAuthorization(
    context: Context
) {
    companion object {
        const val DRIVE_FILE_SCOPE =
            "https://www.googleapis.com/auth/drive.file"
        const val DRIVE_APPDATA_SCOPE =
            "https://www.googleapis.com/auth/drive.appdata"
        const val USERINFO_EMAIL_SCOPE =
            "https://www.googleapis.com/auth/userinfo.email"

        private const val GOOGLE_ACCOUNT_TYPE = "com.google"
    }

    private val appContext = context.applicationContext

    private val authorizationClient: AuthorizationClient =
        Identity.getAuthorizationClient(context)

    fun createAuthorizationRequest(
        forceSelectAccount: Boolean = false
    ): AuthorizationRequest {
        val builder = AuthorizationRequest.builder()
            .setRequestedScopes(
                listOf(
                    Scope(DRIVE_FILE_SCOPE),
                    Scope(DRIVE_APPDATA_SCOPE),
                    Scope(USERINFO_EMAIL_SCOPE)
                )
            )

        /*
         * Credential Manager (used by Ledger Online sign-in) changes the app's
         * default Google account. Pin Drive authorization to the separately
         * verified Drive account so that a later Online login cannot silently
         * redirect Drive sync to the Online Gmail.
         *
         * An explicit Drive-account-change action sets SELECT_ACCOUNT below;
         * Google's API documents that SELECT_ACCOUNT overrides setAccount().
         */
        val driveEmail = DriveAccountBinding.currentEmail(appContext)
        if (!forceSelectAccount && !driveEmail.isNullOrBlank()) {
            builder.setAccount(Account(driveEmail, GOOGLE_ACCOUNT_TYPE))
        }

        if (forceSelectAccount) {
            builder.setPrompt(AuthorizationRequest.Prompt.SELECT_ACCOUNT)
        }

        return builder.build()
    }

    fun authorize(forceSelectAccount: Boolean = false) =
        authorizationClient.authorize(
            createAuthorizationRequest(forceSelectAccount)
        )

    fun authorizationResultFromIntent(data: Intent) =
        authorizationClient.getAuthorizationResultFromIntent(data)
}

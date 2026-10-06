package com.ledger.app.backup

import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope

class GoogleDriveAuthorization(
    context: Context
) {

    companion object {
        const val DRIVE_FILE_SCOPE =
            "https://www.googleapis.com/auth/" + "drive.file"
    }

    private val authorizationClient: AuthorizationClient =
        Identity.getAuthorizationClient(
            context
        )

    fun createAuthorizationRequest(): AuthorizationRequest {
        return AuthorizationRequest
            .builder()
            .setRequestedScopes(
                listOf(
                    Scope(
                        DRIVE_FILE_SCOPE
                    )
                )
            )
            .build()
    }

    fun authorize() =
        authorizationClient.authorize(
            createAuthorizationRequest()
        )
}

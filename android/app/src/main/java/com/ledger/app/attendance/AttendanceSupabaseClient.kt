package com.ledger.app.attendance

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Supabase client dedicated to staff attendance authentication.
 *
 * The custom session key is intentionally different from CloudSupabaseClient's
 * default key so staff sign-in/sign-out cannot replace the Online Groups session.
 */
object AttendanceSupabaseClient {

    private const val SUPABASE_URL =
        "https://tcfplmljllqapezliskb.supabase.co"

    private const val SUPABASE_PUBLISHABLE_KEY =
        "sb_publishable_NFM0I-IV_R23FjaC8aZiWA_xGycrU_g"

    private const val SESSION_STORAGE_KEY =
        "align-ledger-attendance-session-v1"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_PUBLISHABLE_KEY
    ) {
        install(Auth) {
            sessionManager = SettingsSessionManager(
                key = SESSION_STORAGE_KEY
            )
        }
        install(Postgrest)
    }
}

package com.ledger.app.cloudgroup

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object CloudSupabaseClient {

    private const val SUPABASE_URL =
        "https://tcfplmljllqapezliskb.supabase.co"

    private const val SUPABASE_PUBLISHABLE_KEY =
        "sb_publishable_NFM0I-IV_R23FjaC8aZiWA_xGycrU_g"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_PUBLISHABLE_KEY
    ) {
        install(Auth)
        install(Postgrest)
    }
}

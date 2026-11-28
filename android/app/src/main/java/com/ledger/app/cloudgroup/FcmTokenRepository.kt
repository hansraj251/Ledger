
package com.ledger.app.cloudgroup

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object FcmTokenRepository {

    private const val TAG = "FcmTokenRepository"

    suspend fun registerCurrentDevice() {
        try {
            val user = CloudSupabaseClient.client.auth.currentUserOrNull()

            if (user == null) {
                Log.d(TAG, "No Supabase user. Token registration skipped.")
                return
            }

            val token = FirebaseMessaging.getInstance().token.await()

            CloudSupabaseClient.client.postgrest.rpc(
                "register_my_fcm_token",
                buildJsonObject {
                    put("p_token", token)
                    put("p_platform", "android")
                }
            )

            Log.i(TAG, "FCM token registered for authenticated user.")
        } catch (error: Exception) {
            Log.e(TAG, "FCM token registration failed", error)
        }
    }

    suspend fun registerToken(token: String) {
        try {
            val user = CloudSupabaseClient.client.auth.currentUserOrNull()

            if (user == null) {
                Log.d(TAG, "No Supabase user. Token registration skipped.")
                return
            }

            CloudSupabaseClient.client.postgrest.rpc(
                "register_my_fcm_token",
                buildJsonObject {
                    put("p_token", token)
                    put("p_platform", "android")
                }
            )

            Log.i(TAG, "Refreshed FCM token registered.")
        } catch (error: Exception) {
            Log.e(TAG, "FCM token refresh registration failed", error)
        }
    }
}

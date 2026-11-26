package com.ledger.app.attendance

import com.ledger.app.cloudgroup.CloudSupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Owner-side attendance operations use the existing Cloud/Online Groups auth session.
 * Staff authentication remains isolated in AttendanceSupabaseClient.
 */
class AttendanceOwnerRepository {

    private val supabase = CloudSupabaseClient.client

    suspend fun registerStaff(
        localStaffId: Long,
        displayName: String,
        email: String
    ): String {
        check(supabase.auth.currentUserOrNull() != null) {
            "Connect the owner Google account before registering staff attendance."
        }

        return supabase.postgrest.rpc(
            "register_attendance_staff",
            buildJsonObject {
                put("p_local_staff_id", localStaffId)
                put("p_display_name", displayName)
                put("p_email", email.trim())
            }
        ).decodeAs<String>()
    }
}

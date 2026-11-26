package com.ledger.app.attendance

import com.ledger.app.cloudgroup.CloudSupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Locale

/**
 * Owner-side attendance operations use the existing Cloud/Online Groups auth session.
 * Staff authentication remains isolated in AttendanceSupabaseClient.
 */
internal fun isSameAttendanceEmail(ownerEmail: String?, staffEmail: String): Boolean {
    val owner = ownerEmail?.trim()?.lowercase(Locale.ROOT).orEmpty()
    val staff = staffEmail.trim().lowercase(Locale.ROOT)
    return owner.isNotBlank() && staff.isNotBlank() && owner == staff
}

class AttendanceOwnerRepository {

    private val supabase = CloudSupabaseClient.client

    /** Reject the currently connected Ledger Online account as a staff Gmail. */
    fun validateStaffEmail(email: String) {
        val currentUser = supabase.auth.currentUserOrNull()
            ?: error("Connect the owner Google account before registering staff attendance.")

        require(!isSameAttendanceEmail(currentUser.email, email)) {
            "You cannot add your own Ledger Online Gmail as staff. Enter the staff member's Gmail."
        }
    }

    suspend fun registerStaff(
        localStaffId: Long,
        displayName: String,
        email: String
    ): String {
        validateStaffEmail(email)

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

package com.ledger.app.attendance

import com.ledger.app.cloudgroup.CloudSupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class AttendanceStaffRecord(
    val id: String,
    @SerialName("workplace_id") val workplaceId: String,
    @SerialName("local_staff_id") val localStaffId: Long,
    @SerialName("display_name") val displayName: String,
    val email: String,
    @SerialName("email_normalized") val emailNormalized: String,
    @SerialName("auth_user_id") val authUserId: String? = null,
    @SerialName("link_status") val linkStatus: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("linked_at") val linkedAt: String? = null
)

@Serializable
data class AttendanceRecord(
    val id: String,
    @SerialName("staff_id") val staffId: String,
    @SerialName("attendance_date") val attendanceDate: String,
    @SerialName("marked_by_user_id") val markedByUserId: String,
    @SerialName("marked_at") val markedAt: String
)

/**
 * Owner reads use the existing Online Groups auth session.
 * Staff reads/writes use the isolated Attendance auth session.
 */
class AttendanceRepository {
    private val ownerClient = CloudSupabaseClient.client
    private val staffClient = AttendanceSupabaseClient.client

    suspend fun ownerSignedIn(): Boolean =
        ownerClient.auth.currentUserOrNull() != null

    suspend fun fetchOwnerStaff(): List<AttendanceStaffRecord> {
        check(ownerClient.auth.currentUserOrNull() != null) {
            "Connect the owner Google account first."
        }
        return ownerClient.from("attendance_staff")
            .select()
            .decodeList<AttendanceStaffRecord>()
    }

    suspend fun fetchOwnerRecords(): List<AttendanceRecord> {
        check(ownerClient.auth.currentUserOrNull() != null) {
            "Connect the owner Google account first."
        }
        return ownerClient.from("attendance_records")
            .select()
            .decodeList<AttendanceRecord>()
    }

    suspend fun claimStaffLinks(): Int {
        check(staffClient.auth.currentUserOrNull() != null) {
            "Sign in with Google first."
        }
        return staffClient.postgrest
            .rpc("claim_my_attendance_staff", buildJsonObject {})
            .decodeAs<Int>()
    }

    suspend fun fetchMyStaff(): List<AttendanceStaffRecord> {
        val userId = staffClient.auth.currentUserOrNull()?.id
            ?: error("Staff is not signed in.")
        return staffClient.from("attendance_staff")
            .select()
            .decodeList<AttendanceStaffRecord>()
            .filter { it.authUserId == userId && it.isActive && it.linkStatus == "linked" }
    }

    suspend fun fetchMyRecords(): List<AttendanceRecord> {
        check(staffClient.auth.currentUserOrNull() != null) {
            "Staff is not signed in."
        }
        return staffClient.from("attendance_records")
            .select()
            .decodeList<AttendanceRecord>()
    }

    suspend fun markMyAttendance(staffId: String): AttendanceRecord {
        check(staffClient.auth.currentUserOrNull() != null) {
            "Staff is not signed in."
        }
        return staffClient.postgrest
            .rpc(
                "mark_my_attendance",
                kotlinx.serialization.json.buildJsonObject {
                    put("p_staff_id", kotlinx.serialization.json.JsonPrimitive(staffId))
                }
            )
            .decodeList<AttendanceRecord>()
            .firstOrNull()
            ?: error("Attendance was not returned by the server.")
    }
}

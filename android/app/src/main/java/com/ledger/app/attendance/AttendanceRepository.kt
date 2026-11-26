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
    @SerialName("linked_at") val linkedAt: String? = null,
    @SerialName("approval_status") val approvalStatus: String = "pending"
)

@Serializable
data class AttendanceWorkplaceRecord(
    val id: String,
    @SerialName("owner_id") val ownerId: String
)

@Serializable
data class AttendanceInviteRecord(
    @SerialName("staff_id") val staffId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("owner_name") val ownerName: String,
    @SerialName("owner_email") val ownerEmail: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class AttendanceRecord(
    val id: String,
    @SerialName("staff_id") val staffId: String,
    @SerialName("attendance_date") val attendanceDate: String,
    @SerialName("marked_by_user_id") val markedByUserId: String,
    @SerialName("marked_at") val markedAt: String,
    val status: String = "present"
)

/**
 * All attendance operations use the same authenticated Supabase session as
 * Ledger Online / Online Groups. There is no separate attendance login/session.
 */
class AttendanceRepository {
    private val onlineClient = CloudSupabaseClient.client

    suspend fun currentOnlineAccountEmail(): String? =
        onlineClient.auth.currentUserOrNull()?.email

    suspend fun ownerSignedIn(): Boolean =
        onlineClient.auth.currentUserOrNull() != null

    /**
     * A signed-in account is an owner only when it owns an attendance workplace.
     * Checking attendance_staff would be incorrect because a staff member can
     * also read their own accepted staff row.
     */
    suspend fun hasOwnerAttendanceAccess(): Boolean {
        if (onlineClient.auth.currentUserOrNull() == null) return false
        return onlineClient.from("attendance_workplaces")
            .select()
            .decodeList<AttendanceWorkplaceRecord>()
            .isNotEmpty()
    }

    suspend fun fetchOwnerStaff(): List<AttendanceStaffRecord> {
        check(onlineClient.auth.currentUserOrNull() != null) {
            "Connect the owner Google account first."
        }
        return onlineClient.from("attendance_staff")
            .select()
            .decodeList<AttendanceStaffRecord>()
    }

    /**
     * Owner Attendance Report excludes only staff whose registered email matches
     * the currently connected Ledger Online account. This filter is deliberately
     * NOT used by Staff Attendance or the Home card.
     */
    suspend fun fetchOwnerStaffForReport(): List<AttendanceStaffRecord> {
        val onlineAccountEmail = currentOnlineAccountEmail()
            ?.trim()
            ?.lowercase()
            .orEmpty()

        return fetchOwnerStaff().filterNot { staff ->
            val registeredEmail = staff.emailNormalized.ifBlank { staff.email }
                .trim()
                .lowercase()
            onlineAccountEmail.isNotBlank() && registeredEmail == onlineAccountEmail
        }
    }

    suspend fun fetchOwnerRecords(): List<AttendanceRecord> {
        check(onlineClient.auth.currentUserOrNull() != null) {
            "Connect the owner Google account first."
        }
        return onlineClient.from("attendance_records")
            .select()
            .decodeList<AttendanceRecord>()
    }

    /**
     * Deletes only the cloud attendance registration and its attendance history.
     * The local Room staff record is intentionally not touched.
     */
    suspend fun deleteOwnerAttendanceStaff(staffId: String) {
        check(onlineClient.auth.currentUserOrNull() != null) {
            "Connect the owner Google account first."
        }
        onlineClient.postgrest.rpc(
            "delete_owner_attendance_staff",
            buildJsonObject { put("p_staff_id", staffId) }
        ).decodeAs<Int>().also {
            check(it == 1) { "Online attendance staff record was not deleted." }
        }
    }

    suspend fun cancelOwnerAttendanceInvite(staffId: String) {
        check(onlineClient.auth.currentUserOrNull() != null) {
            "Connect the owner Google account first."
        }

        onlineClient.postgrest.rpc(
            "cancel_owner_attendance_invite",
            buildJsonObject { put("p_staff_id", staffId) }
        ).decodeAs<Int>().also {
            check(it == 1) { "Pending attendance invitation was not cancelled." }
        }
    }

    suspend fun setOwnerAttendance(
        staffId: String,
        attendanceDate: String,
        status: String
    ): AttendanceRecord {
        check(onlineClient.auth.currentUserOrNull() != null) {
            "Connect the owner Google account first."
        }
        require(status == "present" || status == "absent") {
            "Attendance status must be Present or Absent."
        }
        return onlineClient.postgrest
            .rpc(
                "set_owner_attendance",
                buildJsonObject {
                    put("p_staff_id", staffId)
                    put("p_attendance_date", attendanceDate)
                    put("p_status", status)
                }
            )
            .decodeList<AttendanceRecord>()
            .firstOrNull()
            ?: error("Updated attendance was not returned by the server.")
    }

    suspend fun claimStaffLinks(): Int {
        check(onlineClient.auth.currentUserOrNull() != null) {
            "Sign in with Google first."
        }
        // This RPC now only counts accepted links. Pending invitations must be
        // explicitly accepted by the staff member before account linking.
        return onlineClient.postgrest
            .rpc("claim_my_attendance_staff", buildJsonObject {})
            .decodeAs<Int>()
    }

    suspend fun fetchMyInvites(): List<AttendanceInviteRecord> {
        check(onlineClient.auth.currentUserOrNull() != null) {
            "Staff is not signed in."
        }
        return onlineClient.postgrest
            .rpc("get_my_attendance_invites", buildJsonObject {})
            .decodeList<AttendanceInviteRecord>()
    }

    suspend fun acceptMyInvite(staffId: String): Int {
        check(onlineClient.auth.currentUserOrNull() != null) {
            "Staff is not signed in."
        }
        return onlineClient.postgrest
            .rpc(
                "accept_my_attendance_invite",
                buildJsonObject { put("p_staff_id", staffId) }
            )
            .decodeAs<Int>()
    }

    suspend fun rejectMyInvite(staffId: String): Int {
        check(onlineClient.auth.currentUserOrNull() != null) {
            "Staff is not signed in."
        }
        return onlineClient.postgrest
            .rpc(
                "reject_my_attendance_invite",
                buildJsonObject { put("p_staff_id", staffId) }
            )
            .decodeAs<Int>()
    }

    /**
     * Accepted staff links used only to decide whether the Home card is available.
     * Do not apply the same-email report filter here: the card must remain visible
     * even when the current Ledger Online account matches the registered staff email.
     */
    suspend fun fetchMyStaffForHomeCard(): List<AttendanceStaffRecord> {
        val userId = onlineClient.auth.currentUserOrNull()?.id
            ?: error("Staff is not signed in.")

        return onlineClient.from("attendance_staff")
            .select()
            .decodeList<AttendanceStaffRecord>()
            .filter { staff ->
                staff.authUserId == userId &&
                    staff.isActive &&
                    staff.linkStatus == "linked" &&
                    staff.approvalStatus == "accepted"
            }
    }

    /**
     * Staff Attendance shows all accepted linked registrations for the signed-in
     * staff account. The owner-report email exclusion must never be applied here.
     */
    suspend fun fetchMyStaff(): List<AttendanceStaffRecord> =
        fetchMyStaffForHomeCard()

    suspend fun fetchMyRecords(): List<AttendanceRecord> {
        check(onlineClient.auth.currentUserOrNull() != null) {
            "Staff is not signed in."
        }
        return onlineClient.from("attendance_records")
            .select()
            .decodeList<AttendanceRecord>()
    }

    suspend fun markMyAttendance(staffId: String): AttendanceRecord {
        check(onlineClient.auth.currentUserOrNull() != null) {
            "Staff is not signed in."
        }
        return onlineClient.postgrest
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

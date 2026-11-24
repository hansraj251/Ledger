package com.ledger.app.cloudgroup

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class CachedCloudGroupDetail(
    val group: CloudGroup,
    val members: List<CloudGroupMember>,
    val currentUserId: String?,
    val expenseData: CloudGroupExpenseData? = null
)

class CloudGroupCache(context: Context) {
    private val prefs = context.getSharedPreferences(
        "ledger_cloud_group_cache",
        Context.MODE_PRIVATE
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun loadLastGroups(): List<CloudGroup>? {
        val userId = prefs.getString(KEY_LAST_USER_ID, null) ?: return null
        val raw = prefs.getString(groupsKey(userId), null) ?: return null

        return runCatching {
            json.decodeFromString<List<CloudGroup>>(raw)
        }.getOrNull()
    }

    fun saveGroups(userId: String, groups: List<CloudGroup>) {
        if (userId.isBlank()) return

        runCatching {
            prefs.edit()
                .putString(KEY_LAST_USER_ID, userId)
                .putString(groupsKey(userId), json.encodeToString(groups))
                .apply()
        }
    }

    fun loadDetail(groupId: String): CachedCloudGroupDetail? {
        val userId = prefs.getString(KEY_LAST_USER_ID, null) ?: return null
        val raw = prefs.getString(detailKey(userId, groupId), null) ?: return null

        return runCatching {
            json.decodeFromString<CachedCloudGroupDetail>(raw)
        }.getOrNull()
    }

    fun saveDetail(
        groupId: String,
        group: CloudGroup?,
        members: List<CloudGroupMember>,
        currentUserId: String?,
        expenseData: CloudGroupExpenseData?
    ) {
        val userId = prefs.getString(KEY_LAST_USER_ID, null) ?: return
        if (group == null) return

        val cached = CachedCloudGroupDetail(
            group = group,
            members = members,
            currentUserId = currentUserId,
            expenseData = expenseData
        )

        runCatching {
            prefs.edit()
                .putString(detailKey(userId, groupId), json.encodeToString(cached))
                .apply()
        }
    }

    private fun groupsKey(userId: String) = "groups_$userId"

    private fun detailKey(userId: String, groupId: String) =
        "detail_${userId}_$groupId"

    private companion object {
        const val KEY_LAST_USER_ID = "last_user_id"
    }
}

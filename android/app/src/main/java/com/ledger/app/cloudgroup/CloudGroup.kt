package com.ledger.app.cloudgroup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CloudGroup(
    val id: String,
    val name: String,
    @SerialName("owner_id")
    val ownerId: String,
    @SerialName("created_at")
    val createdAt: String
)

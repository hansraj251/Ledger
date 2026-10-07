package com.ledger.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class ProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val name: String = "",
    val mobile: String = ""
)

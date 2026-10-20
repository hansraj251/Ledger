package com.ledger.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction

@Entity(
    tableName = "party_sell_items",
    indices = [Index(value = ["partyId"])]
)
data class PartySellItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val partyId: Long,
    val title: String,
    val rate: Double,
    val quantityType: String
)

@Dao
interface PartySellItemDao {
    @Query("SELECT * FROM party_sell_items WHERE partyId = :partyId ORDER BY id")
    suspend fun getByPartyId(partyId: Long): List<PartySellItemEntity>

    @Query("DELETE FROM party_sell_items WHERE partyId = :partyId")
    suspend fun deleteByPartyId(partyId: Long)

    @Insert
    suspend fun insertAll(items: List<PartySellItemEntity>)

    @Transaction
    suspend fun replaceForParty(
        partyId: Long,
        items: List<PartySellItemEntity>
    ) {
        deleteByPartyId(partyId)
        if (items.isNotEmpty()) insertAll(items)
    }
}

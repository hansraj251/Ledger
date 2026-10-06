package com.ledger.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PartyDao {

    @Insert
    fun insert(party: PartyEntity): Long

    @Update
    fun update(party: PartyEntity)

    @Delete
    fun delete(party: PartyEntity)

    @Query("SELECT * FROM parties ORDER BY name COLLATE NOCASE")
    fun getAll(): List<PartyEntity>

    @Query("SELECT * FROM parties WHERE id = :partyId LIMIT 1")
    fun getById(partyId: Long): PartyEntity?

    @Query("SELECT COUNT(*) FROM parties")
    fun count(): Int
}

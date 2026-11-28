package com.ledger.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "write_notes")
data class WriteNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val color: Long = 0xFFFFFFFF,
    val isPinned: Boolean = false,
    val reminderAt: Long? = null,
    val repeatRule: String = "Never",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "write_tasks")
data class WriteTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val details: String = "",
    val isCompleted: Boolean = false,
    val reminderAt: Long? = null,
    val repeatRule: String = "Never",
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface WriteNoteDao {
    @Query("SELECT * FROM write_notes ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<WriteNoteEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(note: WriteNoteEntity): Long
    @Query("DELETE FROM write_notes WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT * FROM write_notes WHERE reminderAt IS NOT NULL") suspend fun reminders(): List<WriteNoteEntity>
}

@Dao
interface WriteTaskDao {
    @Query("SELECT * FROM write_tasks ORDER BY CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END, createdAt DESC")
    fun observeAll(): Flow<List<WriteTaskEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(task: WriteTaskEntity): Long
    @Query("DELETE FROM write_tasks WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT * FROM write_tasks WHERE reminderAt IS NOT NULL") suspend fun reminders(): List<WriteTaskEntity>
    @Query("UPDATE write_tasks SET isCompleted = :completed WHERE id = :id AND repeatRule = 'Never'") suspend fun setCompleted(id: Long, completed: Boolean)
}

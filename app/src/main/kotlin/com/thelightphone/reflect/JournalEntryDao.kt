package com.thelightphone.reflect

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface JournalEntryDao {
    @Insert
    fun insert(entry: JournalEntryEntity): Long

    @Update
    fun update(entry: JournalEntryEntity): Int

    @Delete
    fun delete(entry: JournalEntryEntity): Int

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    fun getEntry(id: Long): JournalEntryEntity?

    @Query("SELECT * FROM journal_entries WHERE promptId = :promptId ORDER BY createdAtEpochMillis DESC")
    fun getEntriesForPrompt(promptId: Int): List<JournalEntryEntity>

    @Query("SELECT * FROM journal_entries ORDER BY createdAtEpochMillis DESC")
    fun getAllEntries(): List<JournalEntryEntity>
}

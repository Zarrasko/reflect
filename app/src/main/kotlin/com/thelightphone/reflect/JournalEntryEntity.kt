package com.thelightphone.reflect

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val promptId: Int,
    val promptText: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val text: String,
    val audioFileName: String?,
    val audioDurationMs: Long,
)

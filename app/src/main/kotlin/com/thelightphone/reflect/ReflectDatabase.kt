package com.thelightphone.reflect

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [JournalEntryEntity::class], version = 1, exportSchema = false)
abstract class ReflectDatabase : RoomDatabase() {
    abstract fun entryDao(): JournalEntryDao
}

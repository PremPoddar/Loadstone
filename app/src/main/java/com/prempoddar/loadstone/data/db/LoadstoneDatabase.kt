package com.prempoddar.loadstone.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [PinEntity::class], version = 2, exportSchema = false)
abstract class LoadstoneDatabase : RoomDatabase() {
    abstract fun pinDao(): PinDao

    companion object {
        /** v1 → v2: add elevation and photo columns to existing pins. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pins ADD COLUMN elevationMeters REAL")
                db.execSQL("ALTER TABLE pins ADD COLUMN photoPaths TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}

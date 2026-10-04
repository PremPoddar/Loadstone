package com.prempoddar.loadstone.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [PinEntity::class], version = 3, exportSchema = false)
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

        /** v2 → v3: existing pins start unpinned. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pins ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}

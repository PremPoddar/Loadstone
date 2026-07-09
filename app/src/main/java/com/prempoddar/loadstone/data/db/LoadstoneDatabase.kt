package com.prempoddar.loadstone.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PinEntity::class], version = 1, exportSchema = false)
abstract class LoadstoneDatabase : RoomDatabase() {
    abstract fun pinDao(): PinDao
}

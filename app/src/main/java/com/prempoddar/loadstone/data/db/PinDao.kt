package com.prempoddar.loadstone.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PinDao {
    @Query("SELECT * FROM pins ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PinEntity>>

    @Query("SELECT * FROM pins WHERE id = :id")
    suspend fun get(id: Long): PinEntity?

    @Upsert
    suspend fun upsert(pin: PinEntity): Long

    @Query("DELETE FROM pins WHERE id = :id")
    suspend fun deleteById(id: Long)
}

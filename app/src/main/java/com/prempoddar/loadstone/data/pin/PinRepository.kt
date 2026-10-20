package com.prempoddar.loadstone.data.pin

import com.prempoddar.loadstone.data.db.PinDao
import com.prempoddar.loadstone.data.db.toDomain
import com.prempoddar.loadstone.data.db.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinRepository @Inject constructor(private val dao: PinDao) {
    fun observeAll(): Flow<List<Pin>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun get(id: Long): Pin? = dao.get(id)?.toDomain()

    suspend fun all(): List<Pin> = dao.all().map { it.toDomain() }

    suspend fun upsert(pin: Pin): Long = dao.upsert(pin.toEntity())

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun setPinned(id: Long, pinned: Boolean) = dao.setPinned(id, pinned)
}

package com.prempoddar.loadstone.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.prempoddar.loadstone.data.pin.Pin
import com.prempoddar.loadstone.data.pin.PinCategory

@Entity(tableName = "pins")
data class PinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val note: String?,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val category: String,
    val createdAt: Long,
)

fun PinEntity.toDomain() = Pin(
    id = id,
    name = name,
    note = note,
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = accuracyMeters,
    category = runCatching { PinCategory.valueOf(category) }.getOrDefault(PinCategory.PIN),
    createdAt = createdAt,
)

fun Pin.toEntity() = PinEntity(
    id = id,
    name = name,
    note = note,
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = accuracyMeters,
    category = category.name,
    createdAt = createdAt,
)

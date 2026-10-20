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
    val elevationMeters: Double? = null,
    // Newline-separated file paths (app-generated names never contain newlines).
    val photoPaths: String = "",
    val isPinned: Boolean = false,
    /** The path walked away from the pin, "lat,lon;lat,lon", for retracing it. */
    val trail: String = "",
    // Newline-separated voice note files, like photoPaths.
    val voiceNotes: String = "",
    val folder: String? = null,
)

private fun encodePhotos(paths: List<String>) = paths.joinToString("\n")
private fun decodePhotos(raw: String) = raw.split("\n").filter { it.isNotBlank() }

fun PinEntity.toDomain() = Pin(
    id = id,
    name = name,
    note = note,
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = accuracyMeters,
    category = runCatching { PinCategory.valueOf(category) }.getOrDefault(PinCategory.PIN),
    elevationMeters = elevationMeters,
    photoPaths = decodePhotos(photoPaths),
    isPinned = isPinned,
    createdAt = createdAt,
    trail = com.prempoddar.loadstone.core.geo.Trail.decode(trail),
    voiceNotes = decodePhotos(voiceNotes),
    folder = folder,
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
    elevationMeters = elevationMeters,
    photoPaths = encodePhotos(photoPaths),
    isPinned = isPinned,
    trail = com.prempoddar.loadstone.core.geo.Trail.encode(trail),
    voiceNotes = encodePhotos(voiceNotes),
    folder = folder?.trim()?.ifBlank { null },
)

package com.prempoddar.loadstone.data.pin

/** Domain model for a saved location the user can navigate back to. */
data class Pin(
    val id: Long = 0,
    val name: String,
    val note: String? = null,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val category: PinCategory = PinCategory.PIN,
    val elevationMeters: Double? = null,
    val photoPaths: List<String> = emptyList(),
    /** Kept at the top of the list regardless of how far away it is. */
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    /** The path walked away from here, from the pin outward; empty if none was recorded. */
    val trail: List<com.prempoddar.loadstone.core.geo.TrailPoint> = emptyList(),
    /** Recorded voice notes, as files in the app's storage. */
    val voiceNotes: List<String> = emptyList(),
    /** A folder the person filed it in ("Trip to Goa", "Parking"), or none. */
    val folder: String? = null,
)

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
    val createdAt: Long = System.currentTimeMillis(),
)

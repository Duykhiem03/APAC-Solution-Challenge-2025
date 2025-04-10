package com.example.childsafe.data.model

import com.google.android.gms.maps.model.LatLng

/**
 * Represents a location destination with details like name, address and distance
 * from current location
 */
data class Destination(
    val id: Long = 0,
    val name: String = "",
    val address: String = "",
    val distance: String = "",
    val coordinates: Coordinates? = null,
    val placeId: String? = null,
    val latLng: LatLng? = null
)

/**
 * Represents geographical coordinates (latitude and longitude)
 */
data class Coordinates(
    val latitude: Double,
    val longitude: Double
) {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
    
    companion object {
        fun fromLatLng(latLng: LatLng): Coordinates = 
            Coordinates(latLng.latitude, latLng.longitude)
    }
}
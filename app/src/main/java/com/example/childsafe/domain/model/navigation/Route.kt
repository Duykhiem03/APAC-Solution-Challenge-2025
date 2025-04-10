package com.example.childsafe.domain.model.navigation

import com.google.android.gms.maps.model.LatLng

/**
 * Represents a complete route from origin to destination.
 */
data class Route(
    val routeId: String,
    val origin: LatLng,
    val destination: LatLng,
    val steps: List<RouteStep>,
    val polyline: String,
    val distance: Distance,
    val duration: Duration,
    val bounds: RouteBounds? = null
) {
    /**
     * Decode the polyline string into a list of LatLng points.
     */
    fun decodedPolyline(): List<LatLng> {
        return com.google.maps.android.PolyUtil.decode(polyline)
    }
}

/**
 * Represents a single step in the route with navigation instructions.
 */
data class RouteStep(
    val instruction: String,
    val distance: Distance,
    val duration: Duration,
    val polyline: String,
    val maneuver: String? = null
) {
    fun decodedPolyline(): List<LatLng> {
        return com.google.maps.android.PolyUtil.decode(polyline)
    }
}

/**
 * Represents distance in meters and formatted text.
 */
data class Distance(
    val meters: Int,
    val text: String
)

/**
 * Represents duration in seconds and formatted text.
 */
data class Duration(
    val seconds: Int,
    val text: String
)

/**
 * Represents the bounds of a route for camera positioning.
 */
data class RouteBounds(
    val northeast: LatLng,
    val southwest: LatLng
)
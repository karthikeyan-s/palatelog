package com.karthik.palatelog.core.location

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {

    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Calculates the great-circle distance between two points on the earth's surface using Haversine formula.
     * @return Distance in kilometers.
     */
    fun calculateDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Calculates a rough bounding box for a given center point and radius in kilometers.
     * Useful for pre-filtering SQLite records before exact Haversine calculation.
     */
    fun getBoundingBox(lat: Double, lon: Double, radiusKm: Double): BoundingBox {
        val latDelta = radiusKm / 111.0
        val lonDelta = radiusKm / (111.0 * cos(Math.toRadians(lat)))

        return BoundingBox(
            minLat = lat - latDelta,
            maxLat = lat + latDelta,
            minLng = lon - lonDelta,
            maxLng = lon + lonDelta
        )
    }

    data class BoundingBox(
        val minLat: Double,
        val maxLat: Double,
        val minLng: Double,
        val maxLng: Double
    )

    fun formatDistance(km: Double): String {
        return if (km < 1.0) {
            "${(km * 1000).toInt()} m"
        } else {
            String.format("%.1f km", km)
        }
    }
}

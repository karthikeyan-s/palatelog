package com.prismpixelvertex.palatelog

import com.prismpixelvertex.palatelog.core.location.GeoUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoUtilsTest {

    @Test
    fun calculateDistance_betweenBangaloreAndMysore_isAccurate() {
        // Bangalore: 12.9716° N, 77.5946° E
        // Mysore: 12.2958° N, 76.6394° E
        val distance = GeoUtils.calculateDistanceKm(12.9716, 77.5946, 12.2958, 76.6394)
        // Expected distance is approximately 128-145 km
        assertTrue("Distance should be around 130-140 km, actual: $distance", distance in 125.0..145.0)
    }

    @Test
    fun formatDistance_formatsCorrectly() {
        val shortDist = GeoUtils.formatDistance(0.45)
        assertEquals("450 m", shortDist)

        val longDist = GeoUtils.formatDistance(12.45)
        assertEquals("12.5 km", longDist)
    }

    @Test
    fun getBoundingBox_enclosesCenterPoint() {
        val bbox = GeoUtils.getBoundingBox(12.9716, 77.5946, 5.0)
        assertTrue(12.9716 in bbox.minLat..bbox.maxLat)
        assertTrue(77.5946 in bbox.minLng..bbox.maxLng)
    }
}

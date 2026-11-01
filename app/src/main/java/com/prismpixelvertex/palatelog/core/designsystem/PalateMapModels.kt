package com.prismpixelvertex.palatelog.core.designsystem

import android.graphics.drawable.Drawable

/**
 * Platform-agnostic marker data contract for PalateLog maps.
 */
data class PalateMapMarker(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val snippet: String = "",
    val customIcon: Drawable? = null
)

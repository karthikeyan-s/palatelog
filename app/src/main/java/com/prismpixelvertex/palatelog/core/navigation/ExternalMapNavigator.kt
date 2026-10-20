package com.prismpixelvertex.palatelog.core.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object ExternalMapNavigator {

    /**
     * Opens turn-by-turn navigation using Google Maps app, any installed map app,
     * or as an ultimate web fallback in a browser.
     * Zero API keys or cloud billing required.
     */
    fun navigateTo(context: Context, latitude: Double, longitude: Double, label: String) {
        val encodedLabel = Uri.encode(label)

        // Tier 1: Try direct Google Maps app driving navigation intent
        val gmmIntentUri = Uri.parse("google.navigation:q=$latitude,$longitude&mode=d")
        val googleMapsIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
        }

        try {
            context.startActivity(googleMapsIntent)
            return
        } catch (_: Exception) {
            // Native Google Maps app intent not available
        }

        // Tier 2: Try standard geo: scheme for any installed map application (Waze, OsmAnd, etc.)
        val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)")
        val geoIntent = Intent(Intent.ACTION_VIEW, geoUri)
        try {
            context.startActivity(geoIntent)
            return
        } catch (_: Exception) {
            // Standard geo intent handler not available
        }

        // Tier 3: Ultimate Fallback - Open directions in Web Browser
        val webMapsUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude")
        val webIntent = Intent(Intent.ACTION_VIEW, webMapsUri)
        try {
            context.startActivity(webIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "No map application or browser found to navigate", Toast.LENGTH_SHORT).show()
        }
    }
}

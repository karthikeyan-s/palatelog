package com.karthik.palatelog.core.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object ExternalMapNavigator {

    /**
     * Opens turn-by-turn navigation in the official Google Maps app for driving directions.
     * Zero API keys or cloud billing required.
     */
    fun navigateTo(context: Context, latitude: Double, longitude: Double, label: String) {
        val encodedLabel = Uri.encode(label)
        // Try Google Navigation intent first
        val gmmIntentUri = Uri.parse("google.navigation:q=$latitude,$longitude&mode=d")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
        }

        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            // Fallback to standard geo URI for any installed map application (Waze, Apple Maps, browser)
            val fallbackUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)")
            val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
            try {
                context.startActivity(fallbackIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "No map application found to navigate", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

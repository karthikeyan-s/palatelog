package com.prismpixelvertex.palatelog.core.navigation

/**
 * Domain interface for triggering turn-by-turn navigation or opening a map application.
 * Decouples navigation intents from platform specific Context/Intent on Android
 * so that Apple Maps (MKMapItem) can be substituted seamlessly on iOS.
 */
interface MapNavigator {
    fun navigateTo(latitude: Double, longitude: Double, label: String)
}

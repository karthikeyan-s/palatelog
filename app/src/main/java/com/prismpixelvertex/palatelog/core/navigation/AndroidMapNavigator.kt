package com.prismpixelvertex.palatelog.core.navigation

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidMapNavigator @Inject constructor(
    @ApplicationContext private val context: Context
) : MapNavigator {

    override fun navigateTo(latitude: Double, longitude: Double, label: String) {
        ExternalMapNavigator.navigateTo(context, latitude, longitude, label)
    }
}

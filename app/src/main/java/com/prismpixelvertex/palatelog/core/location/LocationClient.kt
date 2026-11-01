package com.prismpixelvertex.palatelog.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val placeName: String = "",
    val street: String = "",
    val city: String = "",
    val state: String = ""
)

@Singleton
class LocationClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) : LocationService {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationResult? = withContext(Dispatchers.IO) {
        val cancellationTokenSource = CancellationTokenSource()

        val location: Location? = suspendCancellableCoroutine { continuation ->
            try {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { loc ->
                    continuation.resume(loc)
                }.addOnFailureListener {
                    // Fall back to last location
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        continuation.resume(lastLoc)
                    }.addOnFailureListener {
                        continuation.resume(null)
                    }
                }
            } catch (_: Exception) {
                continuation.resume(null)
            }

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }

        if (location != null) {
            val geocoded = reverseGeocode(location.latitude, location.longitude)
            LocationResult(
                latitude = location.latitude,
                longitude = location.longitude,
                placeName = geocoded.first,
                street = geocoded.second,
                city = geocoded.third
            )
        } else {
            null
        }
    }

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): Triple<String, String, String> =
        withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.firstOrNull()

                if (address != null) {
                    val placeName = address.featureName ?: address.subLocality ?: address.locality ?: ""
                    val street = address.thoroughfare ?: address.subLocality ?: ""
                    val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: ""
                    Triple(placeName, street, city)
                } else {
                    Triple("", "", "")
                }
            } catch (e: Exception) {
                Triple("", "", "")
            }
        }
}

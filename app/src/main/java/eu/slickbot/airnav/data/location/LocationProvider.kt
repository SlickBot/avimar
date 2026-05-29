package eu.slickbot.airnav.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Reads the last-known location from the platform LocationManager. No active
 * listening — we only need a one-shot fix to seed the map view.
 */
class LocationProvider(context: Context) {

  private val appContext = context.applicationContext
  private val manager = appContext
    .getSystemService(Context.LOCATION_SERVICE) as LocationManager

  fun hasPermission(): Boolean {
    val coarse = ContextCompat.checkSelfPermission(
      appContext,
      Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val fine = ContextCompat.checkSelfPermission(
      appContext,
      Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    return coarse || fine
  }

  @SuppressLint("MissingPermission")
  fun lastKnown(): LatLon? {
    if (!hasPermission()) return null
    for (provider in candidateProviders()) {
      val available = runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
      if (!available) continue
      val location = runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
      if (location != null) return LatLon(location.latitude, location.longitude)
    }
    return null
  }

  private fun candidateProviders(): List<String> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
    add(LocationManager.NETWORK_PROVIDER)
    add(LocationManager.GPS_PROVIDER)
  }
}

data class LatLon(val lat: Double, val lon: Double)

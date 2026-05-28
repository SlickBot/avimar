package eu.slickbot.airnav

import android.app.Application
import android.content.Context
import eu.slickbot.airnav.data.adsb.AdsbRepository
import eu.slickbot.airnav.data.adsb.AircraftEnricher
import eu.slickbot.airnav.data.ais.AisRepository
import eu.slickbot.airnav.data.ais.info.VesselEnricher
import eu.slickbot.airnav.data.location.LocationProvider
import eu.slickbot.airnav.data.settings.SecureKeyStore
import eu.slickbot.airnav.data.settings.SettingsRepository

/**
 * Lightweight DI container. Single instance per process — survives config
 * changes, so coroutines and WebSocket connections held by the repositories
 * don't get torn down and re-created on rotation.
 */
class AppContainer(applicationContext: Context) {
    val adsbRepository: AdsbRepository = AdsbRepository()
    val aisRepository: AisRepository = AisRepository()
    val settingsRepository: SettingsRepository = SettingsRepository(applicationContext)
    val secureKeyStore: SecureKeyStore = SecureKeyStore(applicationContext)
    val locationProvider: LocationProvider = LocationProvider(applicationContext)
    val aircraftEnricher: AircraftEnricher = AircraftEnricher()
    val vesselEnricher: VesselEnricher = VesselEnricher()
}

class AirnavApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/** Convenience accessor from any Context. */
fun Context.appContainer(): AppContainer =
    (applicationContext as AirnavApplication).container

package eu.slickbot.airnav

import android.app.Application
import android.content.Context
import eu.slickbot.airnav.data.adsb.AdsbRepository
import eu.slickbot.airnav.data.adsb.AircraftEnricher
import eu.slickbot.airnav.data.ais.AisRepository
import eu.slickbot.airnav.data.ais.info.VesselEnricher
import eu.slickbot.airnav.data.common.HttpTextFetcher
import eu.slickbot.airnav.data.common.JsonTextFetcher
import eu.slickbot.airnav.data.location.LocationProvider
import eu.slickbot.airnav.data.settings.SecureKeyStore
import eu.slickbot.airnav.data.settings.SettingsRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Lightweight DI container. Single instance per process — survives config
 * changes, so coroutines and WebSocket connections held by the repositories
 * don't get torn down and re-created on rotation.
 */
class AppContainer(applicationContext: Context) {
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    val jsonFetcher: JsonTextFetcher = JsonTextFetcher(client = httpClient)
    val textFetcher: HttpTextFetcher = HttpTextFetcher(client = httpClient)

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

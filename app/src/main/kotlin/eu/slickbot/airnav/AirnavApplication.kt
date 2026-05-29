package eu.slickbot.airnav

import android.app.Application
import android.content.Context
import org.maplibre.android.MapLibre
import eu.slickbot.airnav.data.adsb.AdsbRepository
import eu.slickbot.airnav.data.adsb.info.AircraftInfoProvider
import eu.slickbot.airnav.data.adsb.info.adsbdb.AdsbDbClient
import eu.slickbot.airnav.data.adsb.info.airhistory.AirHistoryClient
import eu.slickbot.airnav.data.adsb.info.airhistory.AirHistoryParser
import eu.slickbot.airnav.data.adsb.info.buildAircraftInfoProvider
import eu.slickbot.airnav.data.adsb.info.doc8643.Doc8643Client
import eu.slickbot.airnav.data.adsb.info.doc8643.Doc8643LookupService
import eu.slickbot.airnav.data.adsb.info.doc8643.Doc8643Parser
import eu.slickbot.airnav.data.adsb.info.foxtrotcharlie.FoxtrotCharlieClient
import eu.slickbot.airnav.data.adsb.info.hexdb.HexdbClient
import eu.slickbot.airnav.data.adsb.info.icaodescription.IcaoDescriptionCodeDecoder
import eu.slickbot.airnav.data.adsb.info.typedesignation.TypeDesignatorCatalog
import eu.slickbot.airnav.data.adsb.readsb.AdsbReportEnricher
import eu.slickbot.airnav.data.adsb.readsb.adsbfi.AdsbFiProvider
import eu.slickbot.airnav.data.adsb.readsb.adsblol.AdsbLolProvider
import eu.slickbot.airnav.data.adsb.readsb.airplaneslive.AirplanesLiveProvider
import eu.slickbot.airnav.data.ais.AisRepository
import eu.slickbot.airnav.data.ais.info.VesselEnricher
import eu.slickbot.airnav.data.common.HttpTextFetcher
import eu.slickbot.airnav.data.common.JsonTextFetcher
import eu.slickbot.airnav.data.location.LocationProvider
import eu.slickbot.airnav.data.settings.SecureKeyStore
import eu.slickbot.airnav.data.settings.SettingsRepository
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.time.Duration
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

  val adsbJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
  }

  // minInterval = ZERO disables the per-provider throwing rate limiter
  val adsbFiProvider = AdsbFiProvider(fetcher = jsonFetcher, json = adsbJson, minInterval = Duration.ZERO)
  val adsbLolProvider = AdsbLolProvider(fetcher = jsonFetcher, json = adsbJson, minInterval = Duration.ZERO)
  val airplanesLiveProvider = AirplanesLiveProvider(fetcher = jsonFetcher, json = adsbJson, minInterval = Duration.ZERO)

  val typeDesignatorCatalog: TypeDesignatorCatalog = TypeDesignatorCatalog(adsbJson)
  val icaoDescriptionCodeDecoder: IcaoDescriptionCodeDecoder = IcaoDescriptionCodeDecoder()
  val doc8643Parser: Doc8643Parser = Doc8643Parser()
  val doc8643Client: Doc8643Client = Doc8643Client(fetcher = textFetcher, parser = doc8643Parser)
  val doc8643LookupService: Doc8643LookupService = Doc8643LookupService(
    catalog = typeDesignatorCatalog,
    decoder = icaoDescriptionCodeDecoder,
    cacheTtl = Duration.ofHours(6),
    negativeCacheTtl = Duration.ofMinutes(30),
    onlineFetch = doc8643Client::getAircraft,
  )
  val adsbReportEnricher: AdsbReportEnricher = AdsbReportEnricher(doc8643LookupService)

  val adsbDbClient: AdsbDbClient = AdsbDbClient(fetcher = jsonFetcher, json = adsbJson)
  val hexdbClient: HexdbClient = HexdbClient(
    jsonFetcher = jsonFetcher, textFetcher = textFetcher, json = adsbJson,
  )
  val foxtrotCharlieClient: FoxtrotCharlieClient =
    FoxtrotCharlieClient(fetcher = jsonFetcher, json = adsbJson)
  val airHistoryParser: AirHistoryParser = AirHistoryParser()
  val airHistoryClient: AirHistoryClient =
    AirHistoryClient(fetcher = textFetcher, parser = airHistoryParser)

  val aircraftInfoProvider: AircraftInfoProvider = buildAircraftInfoProvider(
    adsbDbClient = adsbDbClient,
    hexdbClient = hexdbClient,
    foxtrotCharlieClient = foxtrotCharlieClient,
    airHistoryClient = airHistoryClient,
    doc8643LookupService = doc8643LookupService,
    cacheTtl = Duration.ofHours(24),
  )

  val adsbRepository: AdsbRepository = AdsbRepository(
    adsbFi = adsbFiProvider,
    adsbLol = adsbLolProvider,
    airplanesLive = airplanesLiveProvider,
    enricher = adsbReportEnricher,
  )
  val aisRepository: AisRepository = AisRepository()
  val settingsRepository: SettingsRepository = SettingsRepository(applicationContext)
  val secureKeyStore: SecureKeyStore = SecureKeyStore(applicationContext)
  val locationProvider: LocationProvider = LocationProvider(applicationContext)
  val vesselEnricher: VesselEnricher = VesselEnricher()
}

class AirnavApplication : Application() {
  val container: AppContainer by lazy { AppContainer(this) }

  override fun onCreate() {
    super.onCreate()
    MapLibre.getInstance(this)
  }
}

/** Convenience accessor from any Context. */
fun Context.appContainer(): AppContainer =
  (applicationContext as AirnavApplication).container

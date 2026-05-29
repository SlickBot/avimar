package eu.slickbot.airnav.di

import eu.slickbot.airnav.data.adsb.AdsbRepository
import eu.slickbot.airnav.data.adsb.info.buildAircraftInfoProvider
import eu.slickbot.airnav.data.adsb.info.adsbdb.AdsbDbClient
import eu.slickbot.airnav.data.adsb.info.airhistory.AirHistoryClient
import eu.slickbot.airnav.data.adsb.info.airhistory.AirHistoryParser
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
import eu.slickbot.airnav.ui.map.MapViewModel
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.time.Duration
import java.util.concurrent.TimeUnit

val dataModule = module {
  single {
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(15, TimeUnit.SECONDS)
      .build()
  }
  single { JsonTextFetcher(client = get()) }
  single { HttpTextFetcher(client = get()) }

  single {
    Json {
      ignoreUnknownKeys = true
      isLenient = true
      coerceInputValues = true
    }
  }

  // minInterval = ZERO disables the per-provider throwing rate limiter
  single { AdsbFiProvider(fetcher = get(), json = get(), minInterval = Duration.ZERO) }
  single { AdsbLolProvider(fetcher = get(), json = get(), minInterval = Duration.ZERO) }
  single { AirplanesLiveProvider(fetcher = get(), json = get(), minInterval = Duration.ZERO) }

  single { TypeDesignatorCatalog(get()) }
  single { IcaoDescriptionCodeDecoder() }
  single { Doc8643Parser() }
  single { Doc8643Client(fetcher = get(), parser = get()) }
  single {
    Doc8643LookupService(
      catalog = get(),
      decoder = get(),
      cacheTtl = Duration.ofHours(6),
      negativeCacheTtl = Duration.ofMinutes(30),
      onlineFetch = get<Doc8643Client>()::getAircraft,
    )
  }
  single { AdsbReportEnricher(get<Doc8643LookupService>()) }

  single { AdsbDbClient(fetcher = get(), json = get()) }
  single { HexdbClient(jsonFetcher = get(), textFetcher = get(), json = get()) }
  single { FoxtrotCharlieClient(fetcher = get(), json = get()) }
  single { AirHistoryParser() }
  single { AirHistoryClient(fetcher = get(), parser = get()) }

  single {
    buildAircraftInfoProvider(
      adsbDbClient = get(),
      hexdbClient = get(),
      foxtrotCharlieClient = get(),
      airHistoryClient = get(),
      doc8643LookupService = get(),
      cacheTtl = Duration.ofHours(24),
    )
  }

  single {
    AdsbRepository(
      adsbFi = get(),
      adsbLol = get(),
      airplanesLive = get(),
      enricher = get(),
    )
  }
  single { AisRepository() }
  single { SettingsRepository(androidContext()) }
  single { SecureKeyStore(androidContext()) }
  single { LocationProvider(androidContext()) }
  single { VesselEnricher() }
}

val viewModelModule = module {
  viewModel {
    MapViewModel(
      adsbRepo = get(),
      aisRepo = get(),
      settings = get(),
      secureStore = get(),
      locationProvider = get(),
      aircraftInfoProvider = get(),
      vesselEnricher = get(),
    )
  }
}

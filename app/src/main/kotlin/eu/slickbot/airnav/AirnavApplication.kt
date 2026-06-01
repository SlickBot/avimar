package eu.slickbot.airnav

import android.app.Application
import eu.slickbot.airnav.data.settings.SecureKeyStore
import eu.slickbot.airnav.di.dataModule
import eu.slickbot.airnav.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.maplibre.android.MapLibre

class AirnavApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    MapLibre.getInstance(this)
    val koin = startKoin {
      androidContext(this@AirnavApplication)
      modules(dataModule, viewModelModule)
    }.koin
    seedDebugAisApiKey(koin.get())
  }

  // Debug builds: seed SecureKeyStore from .key (BuildConfig.AIS_DEBUG_API_KEY)
  // when no key has been entered yet, so the WS subscription starts on first
  // launch without manually typing it through onboarding.
  private fun seedDebugAisApiKey(store: SecureKeyStore) {
    if (!BuildConfig.DEBUG) return
    val seed = BuildConfig.AIS_DEBUG_API_KEY
    if (seed.isBlank()) return
    if (!store.currentAisApiKey().isNullOrBlank()) return
    store.setAisApiKey(seed)
  }
}

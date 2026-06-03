package eu.slickbot.airnav

import android.app.Application
import eu.slickbot.airnav.di.dataModule
import eu.slickbot.airnav.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.maplibre.android.MapLibre

class AirnavApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    MapLibre.getInstance(this)
    startKoin {
      androidContext(this@AirnavApplication)
      modules(dataModule, viewModelModule)
    }
  }
}

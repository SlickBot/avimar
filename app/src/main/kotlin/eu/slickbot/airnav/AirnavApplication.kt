package eu.slickbot.airnav

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import eu.slickbot.airnav.data.common.BaseHttpFetcher
import eu.slickbot.airnav.di.dataModule
import eu.slickbot.airnav.di.viewModelModule
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.maplibre.android.MapLibre

class AirnavApplication : Application(), ImageLoaderFactory {

  override fun onCreate() {
    super.onCreate()
    MapLibre.getInstance(this)
    startKoin {
      androidContext(this@AirnavApplication)
      modules(dataModule, viewModelModule)
    }
  }

  override fun newImageLoader(): ImageLoader {
    val client = OkHttpClient.Builder()
      .addInterceptor { chain ->
        val request = chain.request().newBuilder()
          .header("User-Agent", BaseHttpFetcher.DEFAULT_BROWSER_USER_AGENT)
          .removeHeader("Referer")
          .build()
        chain.proceed(request)
      }
      .build()
    return ImageLoader.Builder(this)
      .okHttpClient(client)
      .build()
  }
}

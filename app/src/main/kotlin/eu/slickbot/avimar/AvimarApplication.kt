package eu.slickbot.avimar

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import eu.slickbot.avimar.data.common.BaseHttpFetcher
import eu.slickbot.avimar.di.dataModule
import eu.slickbot.avimar.di.viewModelModule
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.maplibre.android.MapLibre

class AviMarApplication : Application(), ImageLoaderFactory {

  override fun onCreate() {
    super.onCreate()
    MapLibre.getInstance(this)
    startKoin {
      androidContext(this@AviMarApplication)
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

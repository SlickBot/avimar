package eu.slickbot.avimar

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import eu.slickbot.avimar.data.common.BaseHttpFetcher
import eu.slickbot.avimar.di.dataModule
import eu.slickbot.avimar.di.viewModelModule
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.maplibre.android.MapLibre

class AviMarApplication : Application(), SingletonImageLoader.Factory {

  override fun onCreate() {
    super.onCreate()
    MapLibre.getInstance(this)
    startKoin {
      androidContext(this@AviMarApplication)
      modules(dataModule, viewModelModule)
    }
  }

  override fun newImageLoader(context: PlatformContext): ImageLoader {
    val client = OkHttpClient.Builder()
      .addInterceptor { chain ->
        val request = chain.request().newBuilder()
          .header("User-Agent", BaseHttpFetcher.DEFAULT_BROWSER_USER_AGENT)
          .removeHeader("Referer")
          .build()
        chain.proceed(request)
      }
      .build()
    return ImageLoader.Builder(context)
      .components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }
      .build()
  }
}

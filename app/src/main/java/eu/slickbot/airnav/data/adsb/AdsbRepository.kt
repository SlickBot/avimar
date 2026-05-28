package eu.slickbot.airnav.data.adsb

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class AdsbRepository(
    private val client: OkHttpClient = defaultClient(),
    private val json: Json = defaultJson(),
) {
    private val adsbFi: AdsbFiApi by lazy {
        Retrofit.Builder()
            .baseUrl(AdsbProvider.AdsbFi.baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AdsbFiApi::class.java)
    }

    suspend fun fetch(
        provider: AdsbProvider,
        lat: Double,
        lon: Double,
        radiusNm: Int,
    ): List<Aircraft> {
        val capped = radiusNm.coerceIn(1, provider.maxRadiusNm)
        val response = when (provider) {
            AdsbProvider.AdsbFi -> adsbFi.aircraftInRange(lat, lon, capped)
            AdsbProvider.AdsbLol,
            AdsbProvider.AirplanesLive -> error("Provider $provider not wired yet")
        }
        return response.aircraft.mapNotNull { it.toAircraftOrNull() }
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        fun defaultJson(): Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }
}

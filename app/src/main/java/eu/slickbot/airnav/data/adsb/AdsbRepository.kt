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
    private val converter by lazy {
        json.asConverterFactory("application/json".toMediaType())
    }

    private val adsbFi: AdsbFiApi by lazy { build(AdsbProvider.AdsbFi.baseUrl) }
    private val adsbLol: AdsbLolApi by lazy { build(AdsbProvider.AdsbLol.baseUrl) }
    private val airplanesLive: AirplanesLiveApi by lazy {
        build(AdsbProvider.AirplanesLive.baseUrl)
    }

    private inline fun <reified T> build(baseUrl: String): T = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(converter)
        .build()
        .create(T::class.java)

    suspend fun fetch(
        provider: AdsbProvider,
        lat: Double,
        lon: Double,
        radiusNm: Int,
    ): List<Aircraft> {
        val capped = radiusNm.coerceIn(1, provider.maxRadiusNm)
        val response = when (provider) {
            AdsbProvider.AdsbFi -> adsbFi.aircraftInRange(lat, lon, capped)
            AdsbProvider.AdsbLol -> adsbLol.aircraftInRange(lat, lon, capped)
            AdsbProvider.AirplanesLive -> airplanesLive.aircraftInRange(lat, lon, capped)
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

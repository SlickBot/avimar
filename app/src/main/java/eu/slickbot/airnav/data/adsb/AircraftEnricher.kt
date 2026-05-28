package eu.slickbot.airnav.data.adsb

import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Looks up static aircraft metadata (registration, owner, type) from
 * adsbdb.com and memoizes results per ICAO hex. Negative results are
 * cached too so unknown airframes don't get retried on every selection.
 */
class AircraftEnricher(
    private val api: AdsbDbApi = defaultApi(),
) {
    private val cache = ConcurrentHashMap<String, Optional>()

    suspend fun lookup(hex: String): AircraftDetails? {
        val normalized = hex.lowercase()
        cache[normalized]?.let { return it.value }
        val details = runCatching { api.aircraft(normalized) }
            .getOrNull()
            ?.response
            ?.aircraft
            ?.let { dto ->
                AircraftDetails(
                    registration = dto.registration?.trim()?.ifEmpty { null },
                    manufacturer = dto.manufacturer?.trim()?.ifEmpty { null },
                    model = (dto.type ?: dto.icaoType)?.trim()?.ifEmpty { null },
                    owner = dto.registeredOwner?.trim()?.ifEmpty { null },
                    countryName = dto.countryName?.trim()?.ifEmpty { null },
                    photoUrl = dto.urlPhoto?.trim()?.ifEmpty { null },
                )
            }
        cache[normalized] = Optional(details)
        return details
    }

    private data class Optional(val value: AircraftDetails?)

    companion object {
        private const val BASE_URL = "https://api.adsbdb.com/"

        fun defaultApi(): AdsbDbApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(AdsbDbApi::class.java)
        }
    }
}

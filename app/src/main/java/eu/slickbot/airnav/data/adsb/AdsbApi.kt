package eu.slickbot.airnav.data.adsb

import retrofit2.http.GET
import retrofit2.http.Path

/**
 * adsb.fi v2 endpoint: /api/v2/lat/{lat}/lon/{lon}/dist/{radiusNm}
 * Max radius is 250 nm.
 */
interface AdsbFiApi {
    @GET("api/v2/lat/{lat}/lon/{lon}/dist/{dist}")
    suspend fun aircraftInRange(
        @Path("lat") lat: Double,
        @Path("lon") lon: Double,
        @Path("dist") radiusNm: Int,
    ): ReadsbResponse
}

package eu.slickbot.airnav.data.adsb

import retrofit2.http.GET
import retrofit2.http.Path

/** adsb.fi: /api/v2/lat/{lat}/lon/{lon}/dist/{nm} */
interface AdsbFiApi {
    @GET("api/v2/lat/{lat}/lon/{lon}/dist/{dist}")
    suspend fun aircraftInRange(
        @Path("lat") lat: Double,
        @Path("lon") lon: Double,
        @Path("dist") radiusNm: Int,
    ): ReadsbResponse
}

/** adsb.lol: /v2/point/{lat}/{lon}/{nm} */
interface AdsbLolApi {
    @GET("v2/point/{lat}/{lon}/{dist}")
    suspend fun aircraftInRange(
        @Path("lat") lat: Double,
        @Path("lon") lon: Double,
        @Path("dist") radiusNm: Int,
    ): ReadsbResponse
}

/** airplanes.live: /v2/point/{lat}/{lon}/{nm} */
interface AirplanesLiveApi {
    @GET("v2/point/{lat}/{lon}/{dist}")
    suspend fun aircraftInRange(
        @Path("lat") lat: Double,
        @Path("lon") lon: Double,
        @Path("dist") radiusNm: Int,
    ): ReadsbResponse
}

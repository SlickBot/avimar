package eu.slickbot.airnav.data.adsb

import retrofit2.http.GET
import retrofit2.http.Path

/** adsbdb.com hex lookup: GET /v0/aircraft/{hex} */
interface AdsbDbApi {
    @GET("v0/aircraft/{hex}")
    suspend fun aircraft(@Path("hex") hex: String): AdsbDbResponse
}

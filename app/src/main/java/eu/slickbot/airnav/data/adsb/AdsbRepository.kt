package eu.slickbot.airnav.data.adsb

import eu.slickbot.airnav.data.adsb.readsb.AdsbReportEnricher
import eu.slickbot.airnav.data.adsb.readsb.adsbfi.AdsbFiProvider
import eu.slickbot.airnav.data.adsb.readsb.adsblol.AdsbLolProvider
import eu.slickbot.airnav.data.adsb.readsb.airplaneslive.AirplanesLiveProvider
import eu.slickbot.airnav.data.adsb.readsb.toAircraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdsbRepository(
  private val adsbFi: AdsbFiProvider,
  private val adsbLol: AdsbLolProvider,
  private val airplanesLive: AirplanesLiveProvider,
  private val enricher: AdsbReportEnricher,
) {

  suspend fun fetch(
    provider: AdsbProvider,
    lat: Double,
    lon: Double,
    radiusNm: Int,
  ): List<Aircraft> {
    return withContext(Dispatchers.IO) {
      val capped = radiusNm.coerceIn(1, provider.maxRadiusNm)
      val query = AdsbCircleQuery(latDeg = lat, lonDeg = lon, radiusNm = capped)
      val snapshot = when (provider) {
        AdsbProvider.AdsbFi -> adsbFi.fetch(query)
        AdsbProvider.AdsbLol -> adsbLol.fetch(query)
        AdsbProvider.AirplanesLive -> airplanesLive.fetch(query)
      }
      enricher.enrich(snapshot).reports.mapNotNull { it.toAircraft() }
    }
  }
}

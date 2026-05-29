package eu.slickbot.airnav.data.ais.info

/**
 * Returns the standard 4-site tracking-link block for an MMSI.
 *
 * Ported from drone-swarm-v9 commander/data/maritime/info/VesselInfoProvider.kt
 * (the `appendTrackingLinks` helper).
 */
internal fun vesselTrackingLinks(mmsi: Long): List<VesselInfoLink> {
  return listOf(
    VesselInfoLink("MarineTraffic", "https://www.marinetraffic.com/en/ais/details/ships/mmsi:$mmsi"),
    VesselInfoLink("VesselFinder", "https://www.vesselfinder.com/vessels/details/$mmsi"),
    VesselInfoLink("MyShipTracking", "https://www.myshiptracking.com/vessels/$mmsi"),
    VesselInfoLink("ShipSpotting", "https://www.shipspotting.com/photos/gallery?mmsi=$mmsi"),
  )
}

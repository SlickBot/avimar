package eu.slickbot.airnav.data.ais.info

internal fun vesselTrackingLinks(mmsi: Long): List<VesselInfoLink> {
  return listOf(
    VesselInfoLink("MarineTraffic", "https://www.marinetraffic.com/en/ais/details/ships/mmsi:$mmsi"),
    VesselInfoLink("VesselFinder", "https://www.vesselfinder.com/vessels/details/$mmsi"),
    VesselInfoLink("MyShipTracking", "https://www.myshiptracking.com/vessels/$mmsi"),
    VesselInfoLink("ShipSpotting", "https://www.shipspotting.com/photos/gallery?mmsi=$mmsi"),
  )
}

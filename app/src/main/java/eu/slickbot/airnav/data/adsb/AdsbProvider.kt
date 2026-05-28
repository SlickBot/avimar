package eu.slickbot.airnav.data.adsb

enum class AdsbProvider(val displayName: String, val baseUrl: String, val maxRadiusNm: Int) {
    AdsbFi("adsb.fi", "https://opendata.adsb.fi/", 250),

    // Reserved for phase-3 wiring.
    AdsbLol("adsb.lol", "https://api.adsb.lol/", 250),
    AirplanesLive("airplanes.live", "https://api.airplanes.live/", 250),
}

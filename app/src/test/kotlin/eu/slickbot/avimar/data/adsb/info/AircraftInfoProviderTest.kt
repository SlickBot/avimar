package eu.slickbot.avimar.data.adsb.info

import eu.slickbot.avimar.data.adsb.info.adsbdb.AdsbDbAircraft
import eu.slickbot.avimar.data.adsb.info.adsbdb.AdsbDbAircraftResponse
import eu.slickbot.avimar.data.adsb.info.adsbdb.AdsbDbAircraftResponseBody
import eu.slickbot.avimar.data.adsb.info.adsbdb.AdsbDbAirport
import eu.slickbot.avimar.data.adsb.info.adsbdb.AdsbDbFlightRoute
import eu.slickbot.avimar.data.adsb.info.airhistory.AirHistoryRegistrationResponse
import eu.slickbot.avimar.data.adsb.info.foxtrotcharlie.FoxtrotCharlieAircraftResponse
import eu.slickbot.avimar.data.adsb.info.hexdb.HexdbAircraftResponse
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class AircraftInfoProviderTest {

  private fun adsbdb(
    reg: String? = null,
    owner: String? = null,
    icaoType: String? = null,
    photo: String? = null,
    route: AdsbDbFlightRoute? = null,
  ): AdsbDbAircraftResponse {
    return AdsbDbAircraftResponse(
      response = AdsbDbAircraftResponseBody(
        aircraft = AdsbDbAircraft(
          registration = reg, icaoType = icaoType, registeredOwner = owner, urlPhoto = photo,
        ),
        flightroute = route,
      ),
    )
  }

  private fun hexdb(
    reg: String? = null,
    type: String? = null,
    owner: String? = null,
    image: String? = null,
  ): HexdbAircraftResponse {
    return HexdbAircraftResponse(
      registration = reg, icaoTypeCode = type, registeredOwners = owner, imageUrl = image,
    )
  }

  private fun foxtrot(
    reg: String? = null,
    type: String? = null,
    owner: String? = null,
  ): FoxtrotCharlieAircraftResponse {
    return FoxtrotCharlieAircraftResponse(
      registration = reg, icaoTypeCode = type, registeredOwners = owner,
    )
  }

  private fun provider(
    adsbdbCall: (String, String?) -> AdsbDbAircraftResponse? = { _, _ -> null },
    hexdbCall: (String) -> HexdbAircraftResponse? = { null },
    foxtrotCall: (String) -> FoxtrotCharlieAircraftResponse? = { null },
    airHistoryCall: (String) -> AirHistoryRegistrationResponse? = { null },
  ): AircraftInfoProvider {
    return AircraftInfoProvider(
      adsbdb = adsbdbCall,
      hexdb = hexdbCall,
      foxtrotCharlie = foxtrotCall,
      airHistory = airHistoryCall,
      cacheTtl = Duration.ofMinutes(30),
    )
  }

  @Test
  fun `fetch returns null for blank icao`() = runTest {
    val r = provider().fetch(
      icao = "", callsign = null, registration = null,
      typeDesignator = null, operator = null, description = null,
    )
    assertNull(r)
  }

  @Test
  fun `fetch merges sources with priority adsbdb over hexdb over foxtrot`() = runTest {
    val p = provider(
      adsbdbCall = { _, _ -> adsbdb(reg = "EI-EBC", icaoType = "B738", owner = "Ryanair", photo = "p.jpg") },
      hexdbCall = { hexdb(reg = "WRONG", type = "WRONG", owner = "WRONG", image = "hexdb.jpg") },
      foxtrotCall = { foxtrot(reg = "WRONG2", type = "WRONG2") },
    )

    val r = p.fetch(
      icao = "4ca873", callsign = null, registration = null,
      typeDesignator = null, operator = null, description = null,
    )!!

    assertEquals("4CA873", r.icao)
    assertEquals("EI-EBC", r.registration)
    assertEquals("B738", r.typeDesignator)
    assertEquals("Ryanair", r.operator)
    assertTrue(r.photoUrls.contains("p.jpg"))
    assertTrue(r.photoUrls.contains("hexdb.jpg"))
  }

  @Test
  fun `doc8643 type image is derived from the type designator`() = runTest {
    val p = provider(
      adsbdbCall = { _, _ -> adsbdb(reg = "HA-LXE", icaoType = "A321", owner = "Wizz Air") },
    )
    val r = p.fetch(
      icao = "471F51", callsign = null, registration = null,
      typeDesignator = null, operator = null, description = null,
    )!!
    assertEquals("A321", r.typeDesignator)
    assertTrue(
      r.modelPhotoUrls.contains("https://doc8643.com/static/img/aircraft/large/A321.jpg"),
    )
  }

  @Test
  fun `adsbdb is called with callsign hint when provided`() = runTest {
    var seenCallsign: String? = "NEVER_SET"
    val p = provider(
      adsbdbCall = { _, cs -> seenCallsign = cs; adsbdb(reg = "EI-EBC") },
    )
    p.fetch(
      icao = "4CA873", callsign = "ryr123", registration = null,
      typeDesignator = null, operator = null, description = null,
    )
    assertEquals("RYR123", seenCallsign)
  }

  @Test
  fun `flight route propagates from adsbdb`() = runTest {
    val route = AdsbDbFlightRoute(
      callsign = "RYR123",
      callsignIcao = "RYR",
      origin = AdsbDbAirport(name = "Dublin", icao = "EIDW"),
      destination = AdsbDbAirport(name = "Stansted", icao = "EGSS"),
    )
    val p = provider(adsbdbCall = { _, _ -> adsbdb(route = route) })

    val r = p.fetch(
      icao = "4CA873", callsign = "RYR123", registration = null,
      typeDesignator = null, operator = null, description = null,
    )!!

    assertNotNull(r.flightRoute)
    assertEquals("EIDW", r.flightRoute!!.origin?.icao)
    assertEquals("EGSS", r.flightRoute.destination?.icao)
  }

  @Test
  fun `tracking links are always appended`() = runTest {
    val p = provider(adsbdbCall = { _, _ -> adsbdb(reg = "EI-EBC") })
    val r = p.fetch(
      icao = "4CA873", callsign = "RYR123", registration = null,
      typeDesignator = null, operator = null, description = null,
    )!!
    val urls = r.infoLinks.map { it.url }
    assertTrue("ADS-B Exchange link expected", urls.any { it.contains("adsbexchange.com") })
    assertTrue("Flightradar24 link expected", urls.any { it.contains("flightradar24.com") })
    assertTrue("AirHistory link expected", urls.any { it.contains("airhistory.net") })
    assertTrue("PlaneSpotters link expected", urls.any { it.contains("planespotters.net") })
  }

  @Test
  fun `second call within ttl returns cached result without re-calling clients`() = runTest {
    var calls = 0
    val p = provider(adsbdbCall = { _, _ -> calls++; adsbdb(reg = "EI-EBC") })

    p.fetch(
      icao = "4CA873", callsign = null, registration = null,
      typeDesignator = null, operator = null, description = null,
    )
    p.fetch(
      icao = "4CA873", callsign = null, registration = null,
      typeDesignator = null, operator = null, description = null,
    )

    assertEquals(1, calls)
  }

  @Test
  fun `all sources failing returns null`() = runTest {
    val p = provider()

    val r = p.fetch(
      icao = "4CA873", callsign = null, registration = null,
      typeDesignator = null, operator = null, description = null,
    )

    assertNull(r)
  }

  @Test
  fun `hint fields are folded in when external sources miss them`() = runTest {
    val p = provider(adsbdbCall = { _, _ -> adsbdb(reg = "EI-EBC") })
    val r = p.fetch(
      icao = "4CA873", callsign = "RYR123", registration = null,
      typeDesignator = null, operator = "Hint Operator", description = "Hint Desc",
    )!!
    assertEquals("Hint Operator", r.operator)
    assertEquals("Hint Desc", r.description)
    assertEquals("RYR123", r.callsign)
  }
}

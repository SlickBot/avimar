package eu.slickbot.airnav.ui.search

import eu.slickbot.airnav.data.adsb.Aircraft
import eu.slickbot.airnav.data.ais.Vessel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchHitTest {

    private val planes = listOf(
        plane("4ca123", callsign = "RYR123", registration = "EI-DAA"),
        plane("a1b2c3", callsign = "AAL45", registration = "N12345"),
        plane("400001", callsign = null, registration = "G-TEST"),
    )

    private val ships = listOf(
        ship(211281610L, name = "ACME CARGO", callsign = "DABC"),
        ship(244987654L, name = "FERRY ONE", callsign = "PBQR"),
        ship(123456789L, name = null, callsign = null),
    )

    @Test
    fun `blank query returns nothing`() {
        assertTrue(searchHits("", planes, ships).isEmpty())
        assertTrue(searchHits("   ", planes, ships).isEmpty())
    }

    @Test
    fun `matches plane by callsign prefix case-insensitively`() {
        val hits = searchHits("ryr", planes, ships)
        assertEquals(1, hits.size)
        assertTrue(hits.single() is SearchHit.Plane)
    }

    @Test
    fun `matches plane by registration substring`() {
        val hits = searchHits("dAA", planes, ships)
        assertEquals(1, hits.size)
        assertTrue(hits.single() is SearchHit.Plane)
    }

    @Test
    fun `matches plane by hex`() {
        val hits = searchHits("400001", planes, ships)
        assertEquals(1, hits.size)
    }

    @Test
    fun `matches ship by name substring`() {
        val hits = searchHits("ferry", planes, ships)
        assertEquals(1, hits.size)
        assertTrue(hits.single() is SearchHit.Ship)
    }

    @Test
    fun `matches ship by MMSI numeric substring`() {
        val hits = searchHits("2112", planes, ships)
        assertEquals(1, hits.size)
    }

    @Test
    fun `planes are returned before ships`() {
        val hits = searchHits("a", planes, ships) // matches AAL45 and ACME CARGO
        assertTrue(hits.first() is SearchHit.Plane)
    }

    @Test
    fun `limit is applied per kind`() {
        val many = (1..20).map { plane(hex = "%06x".format(it), callsign = "FOO$it") }
        val hits = searchHits("foo", many, emptyList(), limitPerKind = 3)
        assertEquals(3, hits.size)
    }

    private fun plane(
        hex: String,
        callsign: String? = null,
        registration: String? = null,
    ) = Aircraft(
        hex = hex,
        callsign = callsign,
        registration = registration,
        type = null,
        lat = 0.0,
        lon = 0.0,
        altitudeFt = null,
        groundSpeedKn = null,
        trackDeg = null,
        squawk = null,
        category = null,
    )

    private fun ship(
        mmsi: Long,
        name: String? = null,
        callsign: String? = null,
    ) = Vessel(
        mmsi = mmsi,
        name = name,
        callsign = callsign,
        imo = null,
        lat = 0.0,
        lon = 0.0,
        sogKn = null,
        cogDeg = null,
        trueHeadingDeg = null,
        navStatus = null,
        shipType = null,
        destination = null,
        updatedAtMs = 0L,
    )
}

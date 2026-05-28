package eu.slickbot.airnav.data.adsb

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadsbAircraftTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun `aircraft without lat or lon is dropped`() {
        val noLat = ReadsbAircraft(hex = "abc123", lat = null, lon = 0.0)
        val noLon = ReadsbAircraft(hex = "abc123", lat = 0.0, lon = null)
        assertNull(noLat.toAircraftOrNull())
        assertNull(noLon.toAircraftOrNull())
    }

    @Test
    fun `alt_baro string "ground" is mapped to zero`() {
        val payload = """
            {"ac":[{"hex":"abc","lat":1.0,"lon":2.0,"alt_baro":"ground"}],"now":0,"total":1}
        """.trimIndent()
        val parsed = json.decodeFromString(ReadsbResponse.serializer(), payload)
        val ac = parsed.aircraft.single().toAircraftOrNull()
        assertNotNull(ac)
        assertEquals(0, ac!!.altitudeFt)
    }

    @Test
    fun `numeric alt_baro is parsed as an integer`() {
        val payload = """
            {"ac":[{"hex":"abc","lat":1.0,"lon":2.0,"alt_baro":37000}],"now":0,"total":1}
        """.trimIndent()
        val ac = json.decodeFromString(ReadsbResponse.serializer(), payload)
            .aircraft.single().toAircraftOrNull()
        assertEquals(37000, ac!!.altitudeFt)
    }

    @Test
    fun `flight callsign is trimmed and blank becomes null`() {
        val trimmed = ReadsbAircraft(hex = "abc", lat = 0.0, lon = 0.0, flight = "  RYR123 ")
            .toAircraftOrNull()
        assertEquals("RYR123", trimmed!!.callsign)

        val blank = ReadsbAircraft(hex = "abc", lat = 0.0, lon = 0.0, flight = "   ")
            .toAircraftOrNull()
        assertNull(blank!!.callsign)
    }

    @Test
    fun `track falls back to true heading when absent`() {
        val ac = ReadsbAircraft(
            hex = "abc",
            lat = 0.0,
            lon = 0.0,
            trackDeg = null,
            trueHeadingDeg = 123.0,
        ).toAircraftOrNull()
        assertEquals(123.0, ac!!.trackDeg)
    }

    @Test
    fun `unknown json fields are ignored`() {
        val payload = """
            {"ac":[{"hex":"abc","lat":1,"lon":2,"unexpected":"foo","nested":{"a":1}}]}
        """.trimIndent()
        val parsed = json.decodeFromString(ReadsbResponse.serializer(), payload)
        assertTrue(parsed.aircraft.isNotEmpty())
    }
}

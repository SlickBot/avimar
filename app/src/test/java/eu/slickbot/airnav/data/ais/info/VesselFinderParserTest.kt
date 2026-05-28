package eu.slickbot.airnav.data.ais.info

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VesselFinderParserTest {

    private val parser = VesselFinderParser()

    @Test
    fun `extracts name, MMSI, flag, type, dimensions, photo from a typical vessel page`() {
        val html = """
            <html><head>
              <meta property="og:image" content="https://static.vesselfinder.net/ship-photo/0-247053540-abcd/1?v1" />
            </head><body>
              <h1>KETTY, Cargo</h1>
              <table>
                <tr><td>MMSI</td><td>247053540</td></tr>
                <tr><td>Call Sign</td><td>IBQR</td></tr>
                <tr><td>Flag</td><td>Italy</td></tr>
                <tr><td>Vessel Type</td><td>General Cargo</td></tr>
                <tr><td>Year Built</td><td>2008</td></tr>
                <tr><td>Gross Tonnage</td><td>4,250</td></tr>
                <tr><td>Length Overall x Breadth Extreme</td><td>89 x 14 m</td></tr>
                <tr><td>Current Draught</td><td>4.8 m</td></tr>
              </table>
              <img class="main-photo" src="https://static.vesselfinder.net/ship-photo/0-247053540-abcd/1?v1" />
            </body></html>
        """.trimIndent()

        val result = parser.parse(html, "https://www.vesselfinder.com/vessels/details/247053540")

        assertEquals("KETTY", result.name)
        assertEquals("IBQR", result.callsign)
        assertEquals("Italy", result.flag)
        assertEquals("General Cargo", result.shipType)
        assertEquals(2008, result.buildYear)
        assertEquals(4250, result.grossTonnage)
        assertEquals(89.0, result.lengthMeters!!, 0.001)
        assertEquals(14.0, result.beamMeters!!, 0.001)
        assertEquals(4.8, result.draughtMeters!!, 0.001)
        assertEquals(
            "https://static.vesselfinder.net/ship-photo/0-247053540-abcd/1?v1",
            result.photoUrl,
        )
    }

    @Test
    fun `filters VesselFinder placeholder photo`() {
        val html = """
            <html><head>
              <meta property="og:image" content="https://static.vesselfinder.net/images/cool-ship2@2.png" />
            </head><body>
              <h1>UNKNOWN, Bulk Carrier</h1>
              <table><tr><td>MMSI</td><td>247053540</td></tr></table>
            </body></html>
        """.trimIndent()
        val result = parser.parse(html, "https://www.vesselfinder.com/vessels/details/247053540")
        assertNull(result.photoUrl)
    }

    @Test
    fun `name strips trailing ship-type suffix after comma`() {
        val html = """<html><body><h1>OCEAN PEARL, Tanker</h1></body></html>"""
        val result = parser.parse(html, "")
        assertEquals("OCEAN PEARL", result.name)
    }
}

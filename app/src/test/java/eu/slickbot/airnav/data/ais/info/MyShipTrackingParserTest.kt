package eu.slickbot.airnav.data.ais.info

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MyShipTrackingParserTest {

    private val parser = MyShipTrackingParser()

    @Test
    fun `parses MMSI, build year, dimensions, draught`() {
        val html = """
            <html><body>
              <h1>NORDIC STAR</h1>
              <h2>Container Ship</h2>
              <table>
                <tr><th>MMSI</th><td>255797000</td></tr>
                <tr><th>IMO</th><td>9287924</td></tr>
                <tr><th>Call Sign</th><td>CQAA</td></tr>
                <tr><th>Flag</th><td>Portugal</td></tr>
                <tr><th>Build</th><td>2005 (21 years old)</td></tr>
                <tr><th>GT</th><td>54,214 Tons</td></tr>
                <tr><th>Size</th><td>294 x 32 m</td></tr>
                <tr><th>Draught</th><td>11.8 m</td></tr>
              </table>
              <img class="ship-photo" src="https://www.myshiptracking.com/uploads/ship-photo/255797000-1.jpg" />
            </body></html>
        """.trimIndent()

        val result = parser.parse(html, "https://www.myshiptracking.com/vessels/255797000")

        assertEquals("NORDIC STAR", result.name)
        assertEquals("Container Ship", result.shipType)
        assertEquals("CQAA", result.callsign)
        assertEquals("Portugal", result.flag)
        assertEquals(2005, result.buildYear)
        assertEquals(54214, result.grossTonnage)
        assertEquals(294.0, result.lengthMeters!!, 0.001)
        assertEquals(32.0, result.beamMeters!!, 0.001)
        assertEquals(11.8, result.draughtMeters!!, 0.001)
        assertNotNull(result.photoUrl)
    }

    @Test
    fun `falls back to h2 for ship type when Type field is missing`() {
        val html = """
            <html><body>
              <h1>EXAMPLE</h1>
              <h2>Bulk Carrier</h2>
              <table><tr><th>MMSI</th><td>1</td></tr></table>
            </body></html>
        """.trimIndent()
        val result = parser.parse(html, "")
        assertEquals("Bulk Carrier", result.shipType)
    }
}

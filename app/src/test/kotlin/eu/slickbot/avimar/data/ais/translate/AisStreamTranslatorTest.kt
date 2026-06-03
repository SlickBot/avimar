package eu.slickbot.avimar.data.ais.translate

import eu.slickbot.avimar.data.ais.AisEnvelope
import eu.slickbot.avimar.data.ais.AisMetaData
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class AisStreamTranslatorTest {

  private fun envelope(
    messageType: String,
    body: JsonElement,
    mmsi: Long? = 211281610L,
    shipName: String? = null,
    lat: Double? = null,
    lon: Double? = null,
    timeUtc: String? = null,
  ): AisEnvelope {
    return AisEnvelope(
      messageType = messageType,
      metaData = AisMetaData(mmsi = mmsi, shipName = shipName, latitude = lat, longitude = lon, timeUtc = timeUtc),
      message = body,
    )
  }

  @Test
  fun `position report yields lat lon sog cog heading nav-status`() {
    val e = envelope(
      messageType = "PositionReport",
      body = buildJsonObject {
        put("PositionReport", buildJsonObject {
          put("Sog", JsonPrimitive(12.3))
          put("Cog", JsonPrimitive(270.0))
          put("TrueHeading", JsonPrimitive(268))
          put("NavigationalStatus", JsonPrimitive(0))
          put("Latitude", JsonPrimitive(53.5))
          put("Longitude", JsonPrimitive(9.9))
        })
      },
    )
    val r = AisStreamTranslator.translate(e)!!
    assertEquals(211281610L, r.mmsi)
    assertEquals(53.5, r.latDeg!!, 0.0)
    assertEquals(9.9, r.lonDeg!!, 0.0)
    assertEquals(12.3 * 0.514444, r.speedOverGroundMs!!, 1e-9)
    assertEquals(270.0, r.courseOverGroundDeg!!, 1e-9)
    assertEquals(268, r.headingDeg)
    assertEquals(0, r.navigationalStatus)
  }

  @Test
  fun `sentinel values are filtered`() {
    val e = envelope(
      messageType = "PositionReport",
      body = buildJsonObject {
        put("PositionReport", buildJsonObject {
          put("Sog", JsonPrimitive(102.3))
          put("Cog", JsonPrimitive(360.0))
          put("TrueHeading", JsonPrimitive(511))
          put("Latitude", JsonPrimitive(1.0))
          put("Longitude", JsonPrimitive(2.0))
        })
      },
    )
    val r = AisStreamTranslator.translate(e)!!
    assertNull("SOG=102.3 sentinel filtered", r.speedOverGroundMs)
    assertNull("COG=360 sentinel filtered", r.courseOverGroundDeg)
    assertNull("heading=511 sentinel filtered", r.headingDeg)
  }

  @Test
  fun `ship static data carries name and IMO`() {
    val e = envelope(
      messageType = "ShipStaticData",
      lat = 53.5, lon = 9.9,
      body = buildJsonObject {
        put("ShipStaticData", buildJsonObject {
          put("ImoNumber", JsonPrimitive(9123456L))
          put("CallSign", JsonPrimitive("DABC"))
          put("Name", JsonPrimitive("ATLANTIC STAR"))
          put("Type", JsonPrimitive(70))
          put("Destination", JsonPrimitive("HAMBURG"))
        })
      },
    )
    val r = AisStreamTranslator.translate(e)!!
    assertEquals("ATLANTIC STAR", r.name)
    assertEquals(9123456L, r.imoNumber)
    assertEquals("DABC", r.callsign)
    assertEquals(70, r.shipType)
    assertEquals("HAMBURG", r.destination)
  }

  @Test
  fun `standard class B position report decodes`() {
    val e = envelope(
      messageType = "StandardClassBPositionReport",
      body = buildJsonObject {
        put("StandardClassBPositionReport", buildJsonObject {
          put("Sog", JsonPrimitive(5.0))
          put("Cog", JsonPrimitive(180.0))
          put("TrueHeading", JsonPrimitive(175))
          put("Latitude", JsonPrimitive(40.0))
          put("Longitude", JsonPrimitive(-70.0))
        })
      },
    )
    val r = AisStreamTranslator.translate(e)!!
    assertEquals(40.0, r.latDeg!!, 0.0)
    assertEquals(-70.0, r.lonDeg!!, 0.0)
    assertEquals(5.0 * 0.514444, r.speedOverGroundMs!!, 1e-9)
    assertEquals(175, r.headingDeg)
  }

  @Test
  fun `extended class B carries name and type`() {
    val e = envelope(
      messageType = "ExtendedClassBPositionReport",
      body = buildJsonObject {
        put("ExtendedClassBPositionReport", buildJsonObject {
          put("Sog", JsonPrimitive(3.5))
          put("Latitude", JsonPrimitive(40.7))
          put("Longitude", JsonPrimitive(-74.0))
          put("Name", JsonPrimitive("LITTLE BOAT"))
          put("Type", JsonPrimitive(36))
        })
      },
    )
    val r = AisStreamTranslator.translate(e)!!
    assertEquals("LITTLE BOAT", r.name)
    assertEquals(36, r.shipType)
  }

  @Test
  fun `missing mmsi yields null translation`() {
    val e = envelope(
      messageType = "PositionReport",
      mmsi = null,
      body = buildJsonObject {
        put("PositionReport", buildJsonObject {
          put("Latitude", JsonPrimitive(1.0))
          put("Longitude", JsonPrimitive(2.0))
        })
      },
    )
    assertNull(AisStreamTranslator.translate(e))
  }

  @Test
  fun `parses aisstream Go-format UTC time`() {
    val e = envelope(
      messageType = "PositionReport",
      timeUtc = "2026-05-28 12:34:56 +0000 UTC",
      body = buildJsonObject {
        put("PositionReport", buildJsonObject {
          put("Latitude", JsonPrimitive(1.0))
          put("Longitude", JsonPrimitive(2.0))
        })
      },
    )
    val r = AisStreamTranslator.translate(e)!!
    assertNotNull(r.timestamp)
    assertEquals(Instant.parse("2026-05-28T12:34:56Z"), r.timestamp)
  }
}

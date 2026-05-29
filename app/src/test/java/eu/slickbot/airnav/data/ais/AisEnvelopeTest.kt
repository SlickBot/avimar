package eu.slickbot.airnav.data.ais

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AisEnvelopeTest {

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
  }

  @Test
  fun `PositionReport envelope deserializes with metadata fields`() {
    val payload = """
            {
              "MessageType": "PositionReport",
              "MetaData": {
                "MMSI": 211281610,
                "ShipName": "TEST SHIP",
                "latitude": 53.5,
                "longitude": 9.9,
                "time_utc": "2026-05-28 12:00:00 +0000 UTC"
              },
              "Message": { "PositionReport": { "Sog": 12.3, "Cog": 270.0, "TrueHeading": 268 } }
            }
        """.trimIndent()
    val env = json.decodeFromString(AisEnvelope.serializer(), payload)
    assertEquals("PositionReport", env.messageType)
    assertEquals(211281610L, env.metaData?.mmsi)
    assertEquals("TEST SHIP", env.metaData?.shipName)
    assertNotNull(env.message)
  }

  @Test
  fun `unknown message types still parse without throwing`() {
    val payload = """
            {"MessageType":"SomethingNew","MetaData":{"MMSI":1},"Message":{"x":1}}
        """.trimIndent()
    val env = json.decodeFromString(AisEnvelope.serializer(), payload)
    assertEquals("SomethingNew", env.messageType)
    assertEquals(1L, env.metaData?.mmsi)
  }

  @Test
  fun `subscription serializes to the AISStream wire format`() {
    val sub = AisStreamSubscription(
      apiKey = "secret",
      boundingBoxes = listOf(listOf(listOf(-1.0, -2.0), listOf(3.0, 4.0))),
    )
    val encoded = json.encodeToString(AisStreamSubscription.serializer(), sub)
    assert(encoded.contains("\"APIKey\":\"secret\""))
    assert(encoded.contains("\"BoundingBoxes\":[[[-1.0,-2.0],[3.0,4.0]]]"))
    assert(encoded.contains("\"FilterMessageTypes\""))
    assert(encoded.contains("PositionReport"))
    assert(encoded.contains("ShipStaticData"))
  }

  @Test
  fun `empty metadata produces null mmsi`() {
    val env = json.decodeFromString(AisEnvelope.serializer(), "{}")
    assertNull(env.metaData)
  }
}

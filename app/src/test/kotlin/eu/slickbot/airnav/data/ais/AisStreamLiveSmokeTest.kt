package eu.slickbot.airnav.data.ais

import eu.slickbot.airnav.data.ais.translate.AisStreamTranslator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class AisStreamLiveSmokeTest {

  @Test
  fun real_websocket_yields_translatable_reports() {
    val gateOn = System.getenv("AIS_SMOKE") == "1" ||
      System.getProperty("airnav.ais.smoke") == "1"
    assumeTrue("AIS_SMOKE=1 not set; skipping live smoke test", gateOn)

    val keyFile = listOf(File(".key"), File("../.key"))
      .firstOrNull { it.exists() }
    assumeTrue(".key not found at ./.key or ../.key", keyFile != null)
    val apiKey = keyFile!!.readText().trim()
    assumeTrue("empty .key", apiKey.isNotEmpty())

    val client = AisStreamClient()
    // Northern Adriatic — busy enough to produce traffic within seconds.
    val bbox = BoundingBox(minLat = 44.0, minLon = 12.0, maxLat = 46.0, maxLon = 15.0)

    val frames = mutableListOf<AisEnvelope>()
    val reports = mutableListOf<AisReport>()

    try {
      runBlocking {
        val collector = launch {
          client.messages.collect { env ->
            frames += env
            AisStreamTranslator.translate(env)?.let { reports += it }
          }
        }
        client.start(apiKey, bbox)
        delay(DURATION_MS)
        collector.cancel()
      }
    } finally {
      client.shutdown()
    }

    println("aisstream live smoke: frames=${frames.size} translated=${reports.size}")
    require(frames.isNotEmpty()) { "no frames received in ${DURATION_MS}ms" }
    require(reports.isNotEmpty()) { "no envelopes translated into AisReport" }
    val withPos = reports.count { it.latDeg != null && it.lonDeg != null }
    require(withPos > 0) { "no reports carried lat/lon — translator regression" }
  }

  private companion object {
    const val DURATION_MS = 20_000L
  }
}

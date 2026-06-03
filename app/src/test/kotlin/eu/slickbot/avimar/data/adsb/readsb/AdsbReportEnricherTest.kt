package eu.slickbot.avimar.data.adsb.readsb

import eu.slickbot.avimar.data.adsb.info.doc8643.Doc8643AircraftDetail
import eu.slickbot.avimar.data.adsb.info.doc8643.Doc8643LookupResponse
import eu.slickbot.avimar.data.adsb.info.doc8643.Doc8643TechnicalData
import eu.slickbot.avimar.data.adsb.info.icaodescription.IcaoDescriptionCodeResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.Instant

private fun report(
  icao: String = "abc",
  typeCode: String? = "B738",
  descCode: String? = null,
  descText: String? = null,
  wtc: String? = null,
): ReadSbReport {
  return ReadSbReport(
    icao = icao,
    callsign = null,
    latDeg = 0.0,
    lonDeg = 0.0,
    altitudeM = null,
    groundSpeedMs = null,
    trackDeg = null,
    lastSeen = Instant.EPOCH,
    icaoTypeCode = typeCode,
    icaoDescriptionCode = descCode,
    icaoDescriptionCodeText = descText,
    wakeTurbulenceCategory = wtc,
  )
}

private fun lookup(
  designator: String,
  classification: String?,
  wtc: String?,
  decodedText: String?,
): Doc8643LookupResponse {
  return Doc8643LookupResponse(
    designator = designator,
    sourceUrl = null,
    cacheTimestampMs = null,
    aircraft = Doc8643AircraftDetail(
      designator = designator,
      classification = classification,
      category = wtc,
      manufacturers = emptyList(),
      tech = Doc8643TechnicalData(),
      imageUrls = emptyList(),
      links = emptyList(),
      canonicalUrl = null,
      image = null,
      title = null,
      description = null,
    ),
    descriptionDecoded = decodedText?.let {
      IcaoDescriptionCodeResult(
        code = classification.orEmpty(),
        valid = true,
        description = it,
        category = "",
        engineCount = "",
        engineType = "",
      )
    },
  )
}

class AdsbReportEnricherTest {

  @Test
  fun `enrich fills in description code, decoded text and WTC`() {
    val snapshot = ReadSbSnapshot(
      reports = listOf(report(typeCode = "B738")),
      capturedAt = Instant.EPOCH,
    )
    val table = mapOf(
      "B738" to lookup("B738", classification = "L2J", wtc = "M", decodedText = "Landplane / 2 / Jet"),
    )
    val enricher = AdsbReportEnricher { designators ->
      designators.mapNotNull { d -> table[d]?.let { d to it } }.toMap()
    }

    val out = enricher.enrich(snapshot)

    val r = out.reports.single()
    assertEquals("L2J", r.icaoDescriptionCode)
    assertEquals("Landplane / 2 / Jet", r.icaoDescriptionCodeText)
    assertEquals("M", r.wakeTurbulenceCategory)
  }

  @Test
  fun `pre-populated fields on the report are preserved`() {
    val snapshot = ReadSbSnapshot(
      reports = listOf(report(typeCode = "B738", descCode = "EXISTING", wtc = "EXISTING_WTC")),
      capturedAt = Instant.EPOCH,
    )
    val table = mapOf(
      "B738" to lookup("B738", classification = "L2J", wtc = "M", decodedText = "x"),
    )
    val enricher = AdsbReportEnricher { designators ->
      designators.mapNotNull { d -> table[d]?.let { d to it } }.toMap()
    }

    val r = enricher.enrich(snapshot).reports.single()
    assertEquals("EXISTING", r.icaoDescriptionCode)
    assertEquals("EXISTING_WTC", r.wakeTurbulenceCategory)
  }

  @Test
  fun `reports without typeCode are left untouched`() {
    val snapshot = ReadSbSnapshot(
      reports = listOf(report(typeCode = null)),
      capturedAt = Instant.EPOCH,
    )
    val enricher = AdsbReportEnricher { _ -> emptyMap() }

    val r = enricher.enrich(snapshot).reports.single()
    assertNull(r.icaoDescriptionCode)
    assertNull(r.wakeTurbulenceCategory)
  }

  @Test
  fun `snapshot with no type codes returns the original snapshot instance`() {
    val snapshot = ReadSbSnapshot(reports = emptyList(), capturedAt = Instant.EPOCH)
    val enricher = AdsbReportEnricher { _ -> emptyMap() }
    assertSame(snapshot, enricher.enrich(snapshot))
  }

  @Test
  fun `lookup failure leaves snapshot intact`() {
    val snapshot = ReadSbSnapshot(
      reports = listOf(report(typeCode = "B738")),
      capturedAt = Instant.EPOCH,
    )
    val enricher = AdsbReportEnricher { _ -> throw RuntimeException("boom") }
    val out = enricher.enrich(snapshot)
    assertNull(out.reports.single().icaoDescriptionCode)
  }
}

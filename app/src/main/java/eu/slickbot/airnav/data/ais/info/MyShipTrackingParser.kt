package eu.slickbot.airnav.data.ais.info

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Best-effort scraper for MyShipTracking vessel pages
 * (https://www.myshiptracking.com/vessels/{MMSI}).
 */
class MyShipTrackingParser {

  fun parse(html: String, sourceUrl: String): VesselDetails {
    val doc = Jsoup.parse(html, sourceUrl)
    val fields = extractFields(doc)

    val shipType = fields["Type"]?.takeIf { it.isNotBlank() }
      ?: doc.selectFirst("h2")?.text()?.trim()?.takeIf { it.isNotBlank() }

    return VesselDetails(
      name = doc.selectFirst("h1")?.text()?.trim()?.takeIf { it.isNotBlank() },
      callsign = fields["Call Sign"]?.takeIf { it.isNotBlank() },
      flag = fields["Flag"]?.takeIf { it.isNotBlank() },
      shipType = shipType,
      buildYear = ParserUtil.buildYear(fields["Build"] ?: fields["Year Built"]),
      grossTonnage = ParserUtil.numericField(fields["GT"] ?: fields["Gross Tonnage"]),
      lengthMeters = ParserUtil.dimension(
        fields["Size"] ?: fields["Length"], isLength = true,
      ),
      beamMeters = ParserUtil.dimension(
        fields["Size"] ?: fields["Beam"], isLength = false,
      ),
      draughtMeters = ParserUtil.decimalField(fields["Draught"] ?: fields["Draft"]),
      photoUrl = extractPhoto(doc),
      sourceUrl = sourceUrl,
    )
  }

  private fun extractFields(doc: Document): Map<String, String> {
    val fields = linkedMapOf<String, String>()
    doc.select("table tr").forEach { tr ->
      val tds = tr.select("td, th")
      if (tds.size >= 2) {
        val label = tds[0].text().trim().removeSuffix(":")
        val value = tds[1].text().trim()
        if (label.isNotBlank() && value.isNotBlank()) fields.putIfAbsent(label, value)
      }
    }
    doc.select("dl").forEach { dl ->
      val dts = dl.select("dt")
      val dds = dl.select("dd")
      for (i in 0 until minOf(dts.size, dds.size)) {
        val label = dts[i].text().trim().removeSuffix(":")
        val value = dds[i].text().trim()
        if (label.isNotBlank() && value.isNotBlank()) fields.putIfAbsent(label, value)
      }
    }
    return fields
  }

  private fun extractPhoto(doc: Document): String? {
    val direct = doc.selectFirst(
      "img.main-photo, img.ship-photo, img.vessel-photo, " +
        "img[src*=/ship-photo/], img[src*=uploads/ship-photo]"
    )?.absUrl("src")
      ?.takeIf { it.isNotBlank() }
      ?.takeUnless { ParserUtil.isPlaceholderPhoto(it) }
    if (direct != null) return direct

    return doc.selectFirst("meta[property=og:image]")?.attr("content")
      ?.takeIf { it.isNotBlank() }
      ?.takeUnless { ParserUtil.isPlaceholderPhoto(it) }
  }
}

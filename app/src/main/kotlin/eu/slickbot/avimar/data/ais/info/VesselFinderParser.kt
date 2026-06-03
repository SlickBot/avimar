package eu.slickbot.avimar.data.ais.info

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class VesselFinderParser {

  fun parse(html: String, sourceUrl: String): VesselDetails {
    val doc = Jsoup.parse(html, sourceUrl)
    val fields = extractFields(doc)
    return VesselDetails(
      name = extractName(doc),
      callsign = field(fields, "Call Sign", "Callsign"),
      flag = field(fields, "Flag", "AIS Flag"),
      shipType = field(fields, "Type", "Vessel Type", "AIS Type"),
      buildYear = ParserUtil.buildYear(field(fields, "Year Built", "Built", "Build")),
      grossTonnage = ParserUtil.numericField(field(fields, "Gross Tonnage", "GT")),
      lengthMeters = ParserUtil.dimension(
        field(fields, "Length Overall x Breadth Extreme", "Length / Beam", "Size"),
        isLength = true,
      ),
      beamMeters = ParserUtil.dimension(
        field(fields, "Length Overall x Breadth Extreme", "Length / Beam", "Size"),
        isLength = false,
      ),
      draughtMeters = ParserUtil.decimalField(
        field(fields, "Current Draught", "Draught", "Draft"),
      ),
      photoUrl = extractPhoto(doc),
    )
  }

  private fun extractName(doc: Document): String? = doc.selectFirst("h1")?.text()
    ?.trim()
    ?.replace(Regex(",\\s*\\w+\\s*$"), "") // strip trailing ", Type"
    ?.takeIf { it.isNotBlank() }

  private fun extractFields(doc: Document): Map<String, String> {
    val fields = linkedMapOf<String, String>()
    doc.select("table tr").forEach { tr ->
      val tds = tr.select("td")
      if (tds.size >= 2) {
        val label = tds[0].text().trim().removeSuffix(":")
        val value = tds[1].text().trim()
        if (label.isNotBlank() && value.isNotBlank()) fields.putIfAbsent(label, value)
      }
    }
    doc.select(".pair, .info-pair, .detail-pair").forEach { pair ->
      val label = pair.selectFirst(".label, .key, dt")?.text()?.trim()?.removeSuffix(":")
      val value = pair.selectFirst(".value, dd")?.text()?.trim()
      if (!label.isNullOrBlank() && !value.isNullOrBlank()) {
        fields.putIfAbsent(label, value)
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

  private fun field(fields: Map<String, String>, vararg keys: String): String? {
    keys.forEach { key ->
      val value = fields.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
      if (!value.isNullOrBlank()) return value
    }
    return null
  }
}

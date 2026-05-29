package eu.slickbot.airnav.data.adsb.info.doc8643

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Jsoup HTML scraper for doc8643.com /aircraft/{DESIGNATOR} pages.
 *
 * Ported verbatim from drone-swarm-v9
 * commander/data/aviation/info/doc8643/Doc8643Parser.kt.
 */
class Doc8643Parser {

  fun parseDetail(
    html: String,
    baseUrl: String,
    requestedDesignator: String,
  ): Doc8643AircraftDetail {
    val doc = Jsoup.parse(html, baseUrl)

    val (designator, classification, category) = parseTopCodesTable(doc, requestedDesignator)
    val manufacturers = parseManufacturers(doc)
    val technicalData = parseTechnicalData(doc)
    val imageUrls = parseImages(doc, baseUrl, designator)
    val links = parseLinks(doc, baseUrl, designator)

    return Doc8643AircraftDetail(
      designator = designator,
      classification = classification,
      category = category,
      manufacturers = manufacturers,
      tech = parseTech(technicalData),
      imageUrls = imageUrls,
      links = links,
      canonicalUrl = doc.selectFirst("link[rel=canonical]")?.absUrl("href")?.ifBlank { null },
      image = doc.selectFirst("meta[property=og:image]")?.attr("content")
        ?.takeIf { it.isNotBlank() }
        ?.let { resolveUrl(baseUrl, it) },
      title = doc.selectFirst("meta[property=og:title]")?.attr("content")?.ifBlank { null },
      description = doc.selectFirst("meta[property=og:description]")?.attr("content")?.ifBlank { null },
    )
  }

  private data class TopCodes(val designator: String, val classification: String?, val category: String?)

  private fun parseTopCodesTable(doc: Document, requestedDesignator: String): TopCodes {
    val designator = doc.selectFirst("div.detail-header h1")
      ?.text()?.trim()?.uppercase()
      ?: extractDesignatorFromOgOrCanonical(doc)
      ?: requestedDesignator.trim().uppercase()

    val badges = doc.select("div.detail-header span.badge")
      .map { it.text().trim() }
      .filter { it.isNotBlank() }

    val classification = badges.getOrNull(0)?.ifBlank { null }
    val category = badges.getOrNull(1)?.ifBlank { null }
    return TopCodes(designator, classification, category)
  }

  private fun extractDesignatorFromOgOrCanonical(doc: Document): String? {
    val ogUrl = doc.selectFirst("meta[property=og:url]")?.attr("content").orEmpty()
    val canonical = doc.selectFirst("link[rel=canonical]")?.attr("href").orEmpty()
    return extractDesignatorFromAircraftUrl(ogUrl) ?: extractDesignatorFromAircraftUrl(canonical)
  }

  private fun parseTechnicalData(doc: Document): Map<String, String> {
    val map = linkedMapOf<String, String>()
    val rows = doc.select("div.tech-data-card .tech-row")
    for (row in rows) {
      val key = row.selectFirst(".tech-label")?.text()?.trim().orEmpty()
      val value = row.selectFirst(".tech-value")?.text()?.trim().orEmpty()
      if (key.isNotBlank() && value.isNotBlank()) {
        map[key] = value
      }
    }
    return map
  }

  private fun parseManufacturers(doc: Document): List<String> {
    return doc.select("div.manuf-card li.list-group-item")
      .map { it.text().trim() }
      .filter { it.isNotBlank() }
      .distinct()
  }

  private fun parseImages(doc: Document, baseUrl: String, designator: String): List<String> {
    val fromDataImg = doc.select("div.detail-images .img-card[data-img]")
      .mapNotNull { it.attr("data-img").trim().takeIf(String::isNotBlank) }
      .map { resolveUrl(baseUrl, it) }
    val fromImgSrc = doc.select("div.detail-images .img-card img[src]")
      .mapNotNull { it.attr("src").trim().takeIf(String::isNotBlank) }
      .map { resolveUrl(baseUrl, it) }
    val og = doc.selectFirst("meta[property=og:image]")?.attr("content")
      ?.takeIf { it.isNotBlank() }
      ?.let { resolveUrl(baseUrl, it) }

    return buildList {
      addAll(fromDataImg)
      addAll(fromImgSrc)
      if (og != null) add(og)
    }
      .filter {
        it.contains("/static/img/aircraft/", ignoreCase = true) ||
          it.contains(designator, ignoreCase = true)
      }
      .distinct()
  }

  private fun parseLinks(doc: Document, baseUrl: String, designator: String): List<Doc8643Link> {
    val ignored = setOf("doc8643", "news", "aircraft", "aircrafts", "gallery", "faq", "contact", "home")
    return doc.select("a[href]")
      .mapNotNull { a ->
        val label = a.text().trim().ifBlank { null } ?: return@mapNotNull null
        if (label.lowercase() in ignored) return@mapNotNull null
        val href = a.absUrl("href").ifBlank { resolveUrl(baseUrl, a.attr("href")) }
        if (href.isBlank()) return@mapNotNull null
        if (href.contains("/aircraft/")) return@mapNotNull null
        if (!href.contains(designator, ignoreCase = true) && !label.contains(designator, ignoreCase = true)) {
          return@mapNotNull null
        }
        Doc8643Link(label = label, url = href)
      }
      .distinctBy { it.url }
  }

  private fun extractDesignatorFromAircraftUrl(url: String): String? {
    val m = Regex("/aircraft/([A-Za-z0-9_-]+)").find(url) ?: return null
    return m.groupValues[1].uppercase()
  }

  private fun resolveUrl(baseUrl: String, url: String): String {
    return when {
      url.startsWith("http://") || url.startsWith("https://") -> url
      url.startsWith("//") -> "https:$url"
      else -> baseUrl.trimEnd('/') + "/" + url.trimStart('/')
    }
  }

  private fun parseTech(values: Map<String, String>): Doc8643TechnicalData {
    return Doc8643TechnicalData(
      wingSpanM = parseDouble(values["Wing Span (m)"]),
      lengthM = parseDouble(values["Length (m)"]),
      heightM = parseDouble(values["Height (m)"]),
      mtowT = parseDouble(values["MTOW (t)"]),
      fuelCapacityL = parseInt(values["Fuel Capacity (ltr)"]),
      maximumRangeNm = parseInt(values["Maximum Range (Nm)"]),
      personsOnBoard = parseInt(values["Persons On Board"]),
      takeOffDistanceM = parseInt(values["Take Off Distance (m)"]),
      landingDistanceM = parseInt(values["Landing Distance (m)"]),
      absoluteCeilingX100Ft = parseInt(values["Absolute Ceiling (x100ft)"]),
      optimumCeilingX100Ft = parseInt(values["Optimum Ceiling (x100ft)"]),
      maximumSpeedKts = parseInt(values["Maximum Speed (kts/M)"]),
      optimumSpeedKts = parseInt(values["Optimum Speed (kts/M)"]),
      maximumClimbRateFtMin = parseInt(values["Maximum Climb Rate (ft/min)"]),
    )
  }

  private fun parseDouble(raw: String?): Double? {
    if (raw.isNullOrBlank()) return null
    return raw.trim().replace(",", "").toDoubleOrNull()
  }

  private fun parseInt(raw: String?): Int? {
    if (raw.isNullOrBlank()) return null
    return raw.trim().replace(",", "").toIntOrNull()
  }
}

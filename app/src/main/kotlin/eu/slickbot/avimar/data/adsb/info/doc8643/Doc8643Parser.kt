package eu.slickbot.avimar.data.adsb.info.doc8643

import eu.slickbot.avimar.data.common.isHttpUrl
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class Doc8643Parser {

  fun parseDetail(
    html: String,
    baseUrl: String,
    requestedDesignator: String,
  ): Doc8643AircraftDetail {
    val doc = Jsoup.parse(html, baseUrl)

    val (designator, classification, category) = parseTopCodesTable(doc, requestedDesignator)
    val techMap = doc.select("div.tech-data-card .tech-row").mapNotNull { row ->
      val label = row.selectFirst(".tech-label")?.text()?.trim().orEmpty()
      val value = row.selectFirst(".tech-value")?.text()?.trim().orEmpty()
      if (label.isNotBlank() && value.isNotBlank()) label to value else null
    }.toMap()

    return Doc8643AircraftDetail(
      designator = designator,
      classification = classification,
      category = category,
      manufacturers = parseManufacturers(doc),
      tech = Doc8643TechnicalData(
        wingSpanM = parseNum(techMap["Wing Span (m)"]),
        lengthM = parseNum(techMap["Length (m)"]),
        heightM = parseNum(techMap["Height (m)"]),
        mtowT = parseNum(techMap["MTOW (t)"]),
        fuelCapacityL = parseInt(techMap["Fuel Capacity (ltr)"]),
        maximumRangeNm = parseInt(techMap["Maximum Range (Nm)"]),
        personsOnBoard = parseInt(techMap["Persons On Board"]),
        takeOffDistanceM = parseInt(techMap["Take Off Distance (m)"]),
        landingDistanceM = parseInt(techMap["Landing Distance (m)"]),
        absoluteCeilingX100Ft = parseInt(techMap["Absolute Ceiling (x100ft)"]),
        optimumCeilingX100Ft = parseInt(techMap["Optimum Ceiling (x100ft)"]),
        maximumSpeedKts = parseInt(techMap["Maximum Speed (kts/M)"]),
        optimumSpeedKts = parseInt(techMap["Optimum Speed (kts/M)"]),
        maximumClimbRateFtMin = parseInt(techMap["Maximum Climb Rate (ft/min)"]),
      ),
      imageUrls = parseImages(doc, baseUrl, designator),
      links = parseLinks(doc, baseUrl, designator),
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
      ?: run {
        val ogUrl = doc.selectFirst("meta[property=og:url]")?.attr("content").orEmpty()
        val canonical = doc.selectFirst("link[rel=canonical]")?.attr("href").orEmpty()
        extractDesignatorFromAircraftUrl(ogUrl) ?: extractDesignatorFromAircraftUrl(canonical)
      }
      ?: requestedDesignator.trim().uppercase()

    val badges = doc.select("div.detail-header span.badge")
      .map { it.text().trim() }
      .filter { it.isNotBlank() }

    return TopCodes(designator, badges.getOrNull(0)?.ifBlank { null }, badges.getOrNull(1)?.ifBlank { null })
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
    }.filter {
      it.contains("/static/img/aircraft/", ignoreCase = true) ||
        it.contains(designator, ignoreCase = true)
    }.distinct()
  }

  private fun parseLinks(doc: Document, baseUrl: String, designator: String): List<Doc8643Link> {
    val ignored = setOf("doc8643", "news", "aircraft", "aircrafts", "gallery", "faq", "contact", "home")
    return doc.select("a[href]")
      .mapNotNull { a ->
        val label = a.text().trim().ifBlank { null } ?: return@mapNotNull null
        if (label.lowercase() in ignored) return@mapNotNull null
        val href = a.absUrl("href").ifBlank { resolveUrl(baseUrl, a.attr("href")) }
        if (href.isBlank()) return@mapNotNull null
        if (!isHttpUrl(href)) return@mapNotNull null
        if (href.contains("/aircraft/")) return@mapNotNull null
        if (!href.contains(designator, ignoreCase = true) && !label.contains(designator, ignoreCase = true)) {
          return@mapNotNull null
        }
        Doc8643Link(label = label, url = href)
      }
      .distinctBy { it.url }
  }

  private fun extractDesignatorFromAircraftUrl(url: String): String? {
    return Regex("/aircraft/([A-Za-z0-9_-]+)").find(url)?.groupValues?.get(1)?.uppercase()
  }

  private fun resolveUrl(baseUrl: String, url: String): String {
    return when {
      url.startsWith("http://") || url.startsWith("https://") -> url
      url.startsWith("//") -> "https:$url"
      else -> baseUrl.trimEnd('/') + "/" + url.trimStart('/')
    }
  }

  private fun parseNum(raw: String?): Double? {
    if (raw.isNullOrBlank()) return null
    return raw.trim().replace(",", "").toDoubleOrNull()
  }

  private fun parseInt(raw: String?): Int? {
    if (raw.isNullOrBlank()) return null
    return raw.trim().replace(",", "").toIntOrNull()
  }
}

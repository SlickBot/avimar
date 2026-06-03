package eu.slickbot.airnav.data.adsb.info.airhistory

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class AirHistoryParser {

  private val dateFormats = listOf(
    DateTimeFormatter.ISO_LOCAL_DATE,
    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH),
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
    DateTimeFormatter.ofPattern("d MMMM, yyyy", Locale.ENGLISH),
    DateTimeFormatter.ofPattern("d MMM, yyyy", Locale.ENGLISH),
  )

  fun parseRegistrationPage(
    html: String,
    baseUrl: String,
    registration: String,
  ): AirHistoryRegistrationResponse {
    val doc = Jsoup.parse(html, baseUrl)
    val blocks = doc.select("div.thumb_separator").toList()
    val photos = blocks.mapNotNull { block -> parsePhotoBlock(block, baseUrl) }
      .distinctBy { it.pageUrl }
      .sortedWith(compareByDescending<AirHistoryPhoto> { it.dateIso ?: "" }.thenBy { it.pageUrl })

    val normReg = registration.trim().uppercase()
    val aircraft = photos.firstOrNull()?.let { first ->
      val tech = first.fields.filterKeys { key ->
        key.startsWith("Registration", ignoreCase = true) ||
          key.startsWith("Aircraft", ignoreCase = true) ||
          key.startsWith("C/n", ignoreCase = true) ||
          key.startsWith("Operator", ignoreCase = true)
      }
      AircraftSummary(
        registration = normReg,
        originalType = tech["Aircraft Original Type:"],
        genericType = tech["Aircraft Generic Type:"],
        version = tech["Aircraft Version:"],
        msn = tech["C/n (msn):"],
        operatorTitles = tech["Operator Titles:"],
        technicalData = tech,
      )
    }

    return AirHistoryRegistrationResponse(
      registration = normReg,
      sourceUrl = baseUrl.trimEnd('/') + "/marks-all/" + normReg,
      aircraft = aircraft,
      photos = photos,
    )
  }

  private fun parsePhotoBlock(block: Element, baseUrl: String): AirHistoryPhoto? {
    val a = block.selectFirst("a[href^=/photo/]") ?: return null
    val pageUrl = a.absUrl("href").ifBlank { resolveUrl(baseUrl, a.attr("href")) }
    val img = block.selectFirst("img[src]")
    val fields = extractCaptionThumbFields(block)
    val dateText = fields["Photo Date:"]
    val dateIso = parseToIso(dateText) ?: extractIsoFromHref(block)
    val city = fields["City / Airport:"]
    val region = fields["Region / Country:"] ?: fields["Country:"]
    val location = listOfNotNull(city, region).joinToString(", ").ifBlank { null }
    val caption = block.selectFirst("div.caption-thumb.normal_text")?.text()?.trim()?.takeIf { it.isNotBlank() }
    val imgUrl = img?.absUrl("src")?.ifBlank { resolveUrl(baseUrl, img.attr("src")) }
    return AirHistoryPhoto(
      pageUrl = pageUrl,
      thumbnailUrl = imgUrl,
      imageUrl = imgUrl,
      photoId = fields["Photo ID:"],
      dateText = dateText,
      dateIso = dateIso,
      photographer = fields["Photo by:"],
      location = location,
      aircraftVersion = fields["Aircraft Version:"],
      operatorTitles = fields["Operator Titles:"],
      caption = caption,
      fields = fields,
    )
  }

  private fun extractCaptionThumbFields(block: Element): Map<String, String> {
    val out = linkedMapOf<String, String>()
    block.select("table.caption-thumb tr").forEach { tr ->
      val tds = tr.select("td")
      if (tds.size < 2) return@forEach
      val key = tds[0].text().trim()
      val value = tds[1].text().trim()
      if (key.isNotBlank() && value.isNotBlank()) out[key] = value
    }
    return out
  }

  private fun parseToIso(dateText: String?): String? {
    if (dateText.isNullOrBlank()) return null
    val cleaned = dateText.trim()
    return dateFormats.firstNotNullOfOrNull { fmt ->
      runCatching { LocalDate.parse(cleaned, fmt).toString() }.getOrNull()
    }
  }

  private fun extractIsoFromHref(block: Element): String? {
    return Regex("date_taken=(\\d{4}-\\d{2}-\\d{2})").find(block.outerHtml())?.groupValues?.get(1)
  }

  private fun resolveUrl(baseUrl: String, url: String): String {
    return when {
      url.startsWith("http://") || url.startsWith("https://") -> url
      url.startsWith("//") -> "https:$url"
      else -> baseUrl.trimEnd('/') + "/" + url.trimStart('/')
    }
  }
}

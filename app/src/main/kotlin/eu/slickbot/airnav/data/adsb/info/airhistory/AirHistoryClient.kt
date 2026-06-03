package eu.slickbot.airnav.data.adsb.info.airhistory

import eu.slickbot.airnav.data.common.HttpTextFetcher
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class AirHistoryClient(
  private val baseUrl: String = "https://www.airhistory.net",
  private val fetcher: HttpTextFetcher,
  private val parser: AirHistoryParser,
) {

  companion object {
    private const val TAG = "airhistory"
  }

  fun getByRegistration(registration: String): AirHistoryRegistrationResponse {
    val normalized = registration.trim().uppercase()
    val encoded = URLEncoder.encode(normalized, StandardCharsets.UTF_8.name()).replace("+", "%20")
    val path = "/marks-all/$encoded"
    val html = fetcher.fetch(TAG, baseUrl, path)
    return parser.parseRegistrationPage(html, baseUrl, normalized)
  }
}

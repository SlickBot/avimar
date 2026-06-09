package eu.slickbot.avimar.ui.map

import kotlin.time.Duration

data class SettingsState(
  val aisApiKey: String?,
  val aisEnabled: Boolean,
  val trailsEnabled: Boolean,
  val aviationVisible: Boolean,
  val maritimeVisible: Boolean,
  val hideGrounded: Boolean,
  val markerScale: Float,
  val adsbPollInterval: Duration,
  val minAltitudeM: Int?,
  val maxAltitudeM: Int?,
  val hiddenAircraftCategories: Set<String>,
  val hiddenVesselCategories: Set<String>,
  val hiddenVesselStatuses: Set<String>,
) {
  companion object {
    val Empty = SettingsState(
      aisApiKey = null,
      aisEnabled = false,
      trailsEnabled = true,
      aviationVisible = true,
      maritimeVisible = true,
      hideGrounded = false,
      markerScale = 1f,
      adsbPollInterval = Duration.ZERO,
      minAltitudeM = null,
      maxAltitudeM = null,
      hiddenAircraftCategories = emptySet(),
      hiddenVesselCategories = emptySet(),
      hiddenVesselStatuses = emptySet(),
    )
  }
}

interface SettingsActions {
  fun setAisApiKey(value: String?)
  fun setAisEnabled(enabled: Boolean)
  fun setTrailsEnabled(enabled: Boolean)
  fun setAviationVisible(visible: Boolean)
  fun setMaritimeVisible(visible: Boolean)
  fun setHideGrounded(hide: Boolean)
  fun setMarkerScale(scale: Float)
  fun setAdsbPollInterval(interval: Duration)
  fun setMinAltitudeM(meters: Int?)
  fun setMaxAltitudeM(meters: Int?)
  fun toggleHiddenAircraftCategory(code: String)
  fun toggleHiddenVesselCategory(code: String)
  fun toggleHiddenVesselStatus(code: String)
}

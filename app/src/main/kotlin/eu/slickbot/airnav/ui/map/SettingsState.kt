package eu.slickbot.airnav.ui.map

/**
 * Snapshot of every value the Settings sheet reads. Bundling them lets the
 * sheet take one [SettingsState] + one [SettingsActions] instead of a dozen
 * scalars and a dozen callbacks each.
 */
data class SettingsState(
  val aisApiKey: String?,
  val aisEnabled: Boolean,
  val trailsEnabled: Boolean,
  val aviationVisible: Boolean,
  val maritimeVisible: Boolean,
  val hideGrounded: Boolean,
  val markerScale: Float,
  val adsbPollMs: Long,
  val adsbTimeoutSec: Int,
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
      adsbPollMs = 0L,
      adsbTimeoutSec = 15,
      minAltitudeM = null,
      maxAltitudeM = null,
      hiddenAircraftCategories = emptySet(),
      hiddenVesselCategories = emptySet(),
      hiddenVesselStatuses = emptySet(),
    )
  }
}

/** Side-effects the Settings sheet can fire. */
interface SettingsActions {
  fun setAisApiKey(value: String?)
  fun setAisEnabled(enabled: Boolean)
  fun setTrailsEnabled(enabled: Boolean)
  fun setAviationVisible(visible: Boolean)
  fun setMaritimeVisible(visible: Boolean)
  fun setHideGrounded(hide: Boolean)
  fun setMarkerScale(scale: Float)
  fun setAdsbPollMs(ms: Long)
  fun setAdsbTimeoutSec(seconds: Int)
  fun setMinAltitudeM(meters: Int?)
  fun setMaxAltitudeM(meters: Int?)
  fun toggleHiddenAircraftCategory(code: String)
  fun toggleHiddenVesselCategory(code: String)
  fun toggleHiddenVesselStatus(code: String)
}

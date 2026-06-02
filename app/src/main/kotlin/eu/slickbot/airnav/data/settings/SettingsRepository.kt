package eu.slickbot.airnav.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import eu.slickbot.airnav.data.adsb.AdsbProvider
import eu.slickbot.airnav.ui.map.TileStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "airnav_settings")

class SettingsRepository(private val context: Context) {

  private val store get() = context.applicationContext.dataStore

  val adsbProvider: Flow<AdsbProvider> = store.data.map { prefs ->
    AdsbProvider.fromId(prefs[KEY_ADSB_PROVIDER])
  }

  val tileStyle: Flow<TileStyle> = store.data.map { prefs ->
    TileStyle.fromId(prefs[KEY_TILE_STYLE])
  }

  val aisEnabled: Flow<Boolean> = store.data.map { prefs ->
    prefs[KEY_AIS_ENABLED] ?: true
  }

  val trailsEnabled: Flow<Boolean> = store.data.map { prefs ->
    prefs[KEY_TRAILS_ENABLED] ?: true
  }

  val onboardingSeen: Flow<Boolean> = store.data.map { prefs ->
    prefs[KEY_ONBOARDING_SEEN] ?: false
  }

  val aviationVisible: Flow<Boolean> = store.data.map { prefs ->
    prefs[KEY_AVIATION_VISIBLE] ?: true
  }

  val maritimeVisible: Flow<Boolean> = store.data.map { prefs ->
    prefs[KEY_MARITIME_VISIBLE] ?: true
  }

  /** Poll interval override in ms; 0 = use the provider's default. */
  val adsbPollMs: Flow<Long> = store.data.map { prefs -> prefs[KEY_ADSB_POLL_MS] ?: 0L }

  /** Per-request timeout in seconds. */
  val adsbTimeoutSec: Flow<Int> = store.data.map { prefs -> prefs[KEY_ADSB_TIMEOUT_SEC] ?: 15 }

  val markerScale: Flow<Float> = store.data.map { prefs -> prefs[KEY_MARKER_SCALE] ?: 1.0f }

  /** Altitude floor in meters, or null for no floor (stored as -1). */
  val minAltitudeM: Flow<Int?> = store.data.map { prefs ->
    prefs[KEY_MIN_ALT_M]?.takeIf { it >= 0 }
  }

  /** Altitude ceiling in meters, or null for no ceiling (stored as -1). */
  val maxAltitudeM: Flow<Int?> = store.data.map { prefs ->
    prefs[KEY_MAX_ALT_M]?.takeIf { it >= 0 }
  }

  val hideGrounded: Flow<Boolean> = store.data.map { prefs -> prefs[KEY_HIDE_GROUNDED] ?: false }

  /** Hidden aircraft categories, by ADS-B emitter category code (e.g. "A7"). */
  val hiddenAircraftCategories: Flow<Set<String>> = store.data.map { prefs ->
    prefs[KEY_HIDDEN_CATEGORIES] ?: emptySet()
  }

  /** Hidden vessel ship-type categories, by [VesselCategory.code] (e.g. "CARGO"). */
  val hiddenVesselCategories: Flow<Set<String>> = store.data.map { prefs ->
    prefs[KEY_HIDDEN_VESSEL_CATEGORIES] ?: emptySet()
  }

  /** Hidden vessel navigational statuses, by [AisStatusCategory.code] (e.g. "5"). */
  val hiddenVesselStatuses: Flow<Set<String>> = store.data.map { prefs ->
    prefs[KEY_HIDDEN_VESSEL_STATUSES] ?: emptySet()
  }

  suspend fun setAdsbProvider(provider: AdsbProvider) {
    store.edit { it[KEY_ADSB_PROVIDER] = provider.id }
  }

  suspend fun setTileStyle(style: TileStyle) {
    store.edit { it[KEY_TILE_STYLE] = style.id }
  }

  suspend fun setAisEnabled(enabled: Boolean) {
    store.edit { it[KEY_AIS_ENABLED] = enabled }
  }

  suspend fun setTrailsEnabled(enabled: Boolean) {
    store.edit { it[KEY_TRAILS_ENABLED] = enabled }
  }

  suspend fun setOnboardingSeen(seen: Boolean) {
    store.edit { it[KEY_ONBOARDING_SEEN] = seen }
  }

  suspend fun setAviationVisible(visible: Boolean) {
    store.edit { it[KEY_AVIATION_VISIBLE] = visible }
  }

  suspend fun setMaritimeVisible(visible: Boolean) {
    store.edit { it[KEY_MARITIME_VISIBLE] = visible }
  }

  suspend fun setAdsbPollMs(ms: Long) {
    store.edit { it[KEY_ADSB_POLL_MS] = ms.coerceAtLeast(0L) }
  }

  suspend fun setAdsbTimeoutSec(seconds: Int) {
    store.edit { it[KEY_ADSB_TIMEOUT_SEC] = seconds.coerceIn(1, 120) }
  }

  suspend fun setMarkerScale(scale: Float) {
    store.edit { it[KEY_MARKER_SCALE] = scale.coerceIn(0.5f, 2.0f) }
  }

  suspend fun setMinAltitudeM(meters: Int?) {
    store.edit { it[KEY_MIN_ALT_M] = meters ?: -1 }
  }

  suspend fun setMaxAltitudeM(meters: Int?) {
    store.edit { it[KEY_MAX_ALT_M] = meters ?: -1 }
  }

  suspend fun setHideGrounded(hide: Boolean) {
    store.edit { it[KEY_HIDE_GROUNDED] = hide }
  }

  suspend fun setHiddenAircraftCategories(categories: Set<String>) {
    store.edit { it[KEY_HIDDEN_CATEGORIES] = categories }
  }

  suspend fun setHiddenVesselCategories(categories: Set<String>) {
    store.edit { it[KEY_HIDDEN_VESSEL_CATEGORIES] = categories }
  }

  suspend fun setHiddenVesselStatuses(statuses: Set<String>) {
    store.edit { it[KEY_HIDDEN_VESSEL_STATUSES] = statuses }
  }

  private companion object {
    val KEY_ADSB_PROVIDER = stringPreferencesKey("adsb_provider")
    val KEY_TILE_STYLE = stringPreferencesKey("tile_style")
    val KEY_AIS_ENABLED = booleanPreferencesKey("ais_enabled")
    val KEY_TRAILS_ENABLED = booleanPreferencesKey("trails_enabled")
    val KEY_ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
    val KEY_AVIATION_VISIBLE = booleanPreferencesKey("aviation_visible")
    val KEY_MARITIME_VISIBLE = booleanPreferencesKey("maritime_visible")
    val KEY_ADSB_POLL_MS = longPreferencesKey("adsb_poll_ms")
    val KEY_ADSB_TIMEOUT_SEC = intPreferencesKey("adsb_timeout_sec")
    val KEY_MARKER_SCALE = floatPreferencesKey("marker_scale")
    val KEY_MIN_ALT_M = intPreferencesKey("min_altitude_m")
    val KEY_MAX_ALT_M = intPreferencesKey("max_altitude_m")
    val KEY_HIDE_GROUNDED = booleanPreferencesKey("hide_grounded")
    val KEY_HIDDEN_CATEGORIES = stringSetPreferencesKey("hidden_categories")
    val KEY_HIDDEN_VESSEL_CATEGORIES = stringSetPreferencesKey("hidden_vessel_categories")
    val KEY_HIDDEN_VESSEL_STATUSES = stringSetPreferencesKey("hidden_vessel_statuses")
  }
}

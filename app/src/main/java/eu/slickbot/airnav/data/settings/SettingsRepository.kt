package eu.slickbot.airnav.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
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

  /** Altitude floor in feet, or null for no floor (stored as -1). */
  val minAltitudeFt: Flow<Int?> = store.data.map { prefs ->
    prefs[KEY_MIN_ALT_FT]?.takeIf { it >= 0 }
  }

  /** Altitude ceiling in feet, or null for no ceiling (stored as -1). */
  val maxAltitudeFt: Flow<Int?> = store.data.map { prefs ->
    prefs[KEY_MAX_ALT_FT]?.takeIf { it >= 0 }
  }

  val hideGrounded: Flow<Boolean> = store.data.map { prefs -> prefs[KEY_HIDE_GROUNDED] ?: false }

  /** Hidden aircraft categories, by ADS-B emitter category code (e.g. "A7"). */
  val hiddenAircraftCategories: Flow<Set<String>> = store.data.map { prefs ->
    prefs[KEY_HIDDEN_CATEGORIES] ?: emptySet()
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

  suspend fun setMinAltitudeFt(ft: Int?) {
    store.edit { it[KEY_MIN_ALT_FT] = ft ?: -1 }
  }

  suspend fun setMaxAltitudeFt(ft: Int?) {
    store.edit { it[KEY_MAX_ALT_FT] = ft ?: -1 }
  }

  suspend fun setHideGrounded(hide: Boolean) {
    store.edit { it[KEY_HIDE_GROUNDED] = hide }
  }

  suspend fun setHiddenAircraftCategories(categories: Set<String>) {
    store.edit { it[KEY_HIDDEN_CATEGORIES] = categories }
  }

  private companion object {
    val KEY_ADSB_PROVIDER: Preferences.Key<String> = stringPreferencesKey("adsb_provider")
    val KEY_TILE_STYLE: Preferences.Key<String> = stringPreferencesKey("tile_style")
    val KEY_AIS_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("ais_enabled")
    val KEY_TRAILS_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("trails_enabled")
    val KEY_ONBOARDING_SEEN: Preferences.Key<Boolean> = booleanPreferencesKey("onboarding_seen")
    val KEY_AVIATION_VISIBLE: Preferences.Key<Boolean> = booleanPreferencesKey("aviation_visible")
    val KEY_MARITIME_VISIBLE: Preferences.Key<Boolean> = booleanPreferencesKey("maritime_visible")
    val KEY_ADSB_POLL_MS: Preferences.Key<Long> = longPreferencesKey("adsb_poll_ms")
    val KEY_ADSB_TIMEOUT_SEC: Preferences.Key<Int> = intPreferencesKey("adsb_timeout_sec")
    val KEY_MARKER_SCALE: Preferences.Key<Float> = floatPreferencesKey("marker_scale")
    val KEY_MIN_ALT_FT: Preferences.Key<Int> = intPreferencesKey("min_altitude_ft")
    val KEY_MAX_ALT_FT: Preferences.Key<Int> = intPreferencesKey("max_altitude_ft")
    val KEY_HIDE_GROUNDED: Preferences.Key<Boolean> = booleanPreferencesKey("hide_grounded")
    val KEY_HIDDEN_CATEGORIES: Preferences.Key<Set<String>> = stringSetPreferencesKey("hidden_categories")
  }
}

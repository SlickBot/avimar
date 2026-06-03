package eu.slickbot.avimar.data.settings

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
import eu.slickbot.avimar.data.adsb.AdsbProvider
import eu.slickbot.avimar.ui.map.TileStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val Context.dataStore by preferencesDataStore(name = "avimar_settings")

class SettingsRepository(private val context: Context) {

  private val store get() = context.applicationContext.dataStore

  private fun <T> read(key: Preferences.Key<T>, default: T): Flow<T> {
    return store.data.map { it[key] ?: default }
  }

  private suspend fun <T> write(key: Preferences.Key<T>, value: T) {
    store.edit { it[key] = value }
  }

  val adsbProvider: Flow<AdsbProvider> = store.data.map { AdsbProvider.fromId(it[KEY_ADSB_PROVIDER]) }
  val tileStyle: Flow<TileStyle> = store.data.map { TileStyle.fromId(it[KEY_TILE_STYLE]) }
  val aisEnabled: Flow<Boolean> = read(KEY_AIS_ENABLED, true)
  val trailsEnabled: Flow<Boolean> = read(KEY_TRAILS_ENABLED, true)
  val aviationVisible: Flow<Boolean> = read(KEY_AVIATION_VISIBLE, true)
  val maritimeVisible: Flow<Boolean> = read(KEY_MARITIME_VISIBLE, true)
  val adsbPollInterval: Flow<Duration> = read(KEY_ADSB_POLL_MS, 0L).map { it.milliseconds }
  val requestTimeout: Flow<Duration> = read(KEY_ADSB_TIMEOUT_SEC, 15).map { it.seconds }
  val markerScale: Flow<Float> = read(KEY_MARKER_SCALE, 1.0f)
  val minAltitudeM: Flow<Int?> = store.data.map { it[KEY_MIN_ALT_M]?.takeIf { v -> v >= 0 } }
  val maxAltitudeM: Flow<Int?> = store.data.map { it[KEY_MAX_ALT_M]?.takeIf { v -> v >= 0 } }
  val hideGrounded: Flow<Boolean> = read(KEY_HIDE_GROUNDED, false)
  val hiddenAircraftCategories: Flow<Set<String>> = read(KEY_HIDDEN_CATEGORIES, emptySet())
  val hiddenVesselCategories: Flow<Set<String>> = read(KEY_HIDDEN_VESSEL_CATEGORIES, emptySet())
  val hiddenVesselStatuses: Flow<Set<String>> = read(KEY_HIDDEN_VESSEL_STATUSES, emptySet())

  suspend fun setAdsbProvider(provider: AdsbProvider) { write(KEY_ADSB_PROVIDER, provider.id) }
  suspend fun setTileStyle(style: TileStyle) { write(KEY_TILE_STYLE, style.id) }
  suspend fun setAisEnabled(enabled: Boolean) { write(KEY_AIS_ENABLED, enabled) }
  suspend fun setTrailsEnabled(enabled: Boolean) { write(KEY_TRAILS_ENABLED, enabled) }
  suspend fun setAviationVisible(visible: Boolean) { write(KEY_AVIATION_VISIBLE, visible) }
  suspend fun setMaritimeVisible(visible: Boolean) { write(KEY_MARITIME_VISIBLE, visible) }

  suspend fun setAdsbPollInterval(interval: Duration) { write(KEY_ADSB_POLL_MS, interval.inWholeMilliseconds.coerceAtLeast(0L)) }
  suspend fun setRequestTimeout(timeout: Duration) { write(KEY_ADSB_TIMEOUT_SEC, timeout.inWholeSeconds.toInt().coerceIn(1, 120)) }
  suspend fun setMarkerScale(scale: Float) { write(KEY_MARKER_SCALE, scale.coerceIn(0.5f, 2.0f)) }

  suspend fun setMinAltitudeM(meters: Int?) { write(KEY_MIN_ALT_M, meters ?: -1) }
  suspend fun setMaxAltitudeM(meters: Int?) { write(KEY_MAX_ALT_M, meters ?: -1) }

  suspend fun setHideGrounded(hide: Boolean) { write(KEY_HIDE_GROUNDED, hide) }
  suspend fun setHiddenAircraftCategories(categories: Set<String>) { write(KEY_HIDDEN_CATEGORIES, categories) }
  suspend fun setHiddenVesselCategories(categories: Set<String>) { write(KEY_HIDDEN_VESSEL_CATEGORIES, categories) }
  suspend fun setHiddenVesselStatuses(statuses: Set<String>) { write(KEY_HIDDEN_VESSEL_STATUSES, statuses) }

  private companion object {
    val KEY_ADSB_PROVIDER = stringPreferencesKey("adsb_provider")
    val KEY_TILE_STYLE = stringPreferencesKey("tile_style")
    val KEY_AIS_ENABLED = booleanPreferencesKey("ais_enabled")
    val KEY_TRAILS_ENABLED = booleanPreferencesKey("trails_enabled")
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

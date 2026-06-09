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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val Context.dataStore by preferencesDataStore(name = "avimar_settings")

class SettingsRepository(
  private val context: Context,
  private val cipher: StringCipher = KeystoreStringCipher(),
) {

  private val store get() = context.applicationContext.dataStore

  val aisApiKey: Flow<String?> = store.data
    .map { it[AIS_API_KEY] }
    .distinctUntilChanged()
    .map { stored -> stored?.let(cipher::decrypt) }
  val adsbProvider: Flow<AdsbProvider> = store.data.map { AdsbProvider.fromId(it[ADSB_PROVIDER]) }
  val tileStyle: Flow<TileStyle> = store.data.map { TileStyle.fromId(it[TILE_STYLE]) }
  val aisEnabled: Flow<Boolean> = read(AIS_ENABLED, true)
  val trailsEnabled: Flow<Boolean> = read(TRAILS_ENABLED, true)
  val aviationVisible: Flow<Boolean> = read(AVIATION_VISIBLE, true)
  val maritimeVisible: Flow<Boolean> = read(MARITIME_VISIBLE, true)
  val adsbPollInterval: Flow<Duration> = read(ADSB_POLL_MS, 0L).map { it.milliseconds }
  val markerScale: Flow<Float> = read(MARKER_SCALE, 1.0f)
  val minAltitudeM: Flow<Int?> = store.data.map { it[MIN_ALT_M]?.takeIf { v -> v >= 0 } }
  val maxAltitudeM: Flow<Int?> = store.data.map { it[MAX_ALT_M]?.takeIf { v -> v >= 0 } }
  val hideGrounded: Flow<Boolean> = read(HIDE_GROUNDED, false)
  val hiddenAircraftCategories: Flow<Set<String>> = read(HIDDEN_CATEGORIES, emptySet())
  val hiddenVesselCategories: Flow<Set<String>> = read(HIDDEN_VESSEL_CATEGORIES, emptySet())
  val hiddenVesselStatuses: Flow<Set<String>> = read(HIDDEN_VESSEL_STATUSES, emptySet())

  suspend fun setAisApiKey(value: String?) {
    val trimmed = value?.trim()
    if (trimmed.isNullOrBlank()) {
      store.edit { it.remove(AIS_API_KEY) }
    } else {
      write(AIS_API_KEY, cipher.encrypt(trimmed))
    }
  }

  suspend fun setAdsbProvider(provider: AdsbProvider) { write(ADSB_PROVIDER, provider.id) }
  suspend fun setTileStyle(style: TileStyle) { write(TILE_STYLE, style.id) }
  suspend fun setAisEnabled(enabled: Boolean) { write(AIS_ENABLED, enabled) }
  suspend fun setTrailsEnabled(enabled: Boolean) { write(TRAILS_ENABLED, enabled) }
  suspend fun setAviationVisible(visible: Boolean) { write(AVIATION_VISIBLE, visible) }
  suspend fun setMaritimeVisible(visible: Boolean) { write(MARITIME_VISIBLE, visible) }

  suspend fun setAdsbPollInterval(interval: Duration) { write(ADSB_POLL_MS, interval.inWholeMilliseconds.coerceAtLeast(0L)) }
  suspend fun setMarkerScale(scale: Float) { write(MARKER_SCALE, scale.coerceIn(0.5f, 2.0f)) }

  suspend fun setMinAltitudeM(meters: Int?) { write(MIN_ALT_M, meters ?: -1) }
  suspend fun setMaxAltitudeM(meters: Int?) { write(MAX_ALT_M, meters ?: -1) }

  suspend fun setHideGrounded(hide: Boolean) { write(HIDE_GROUNDED, hide) }
  suspend fun setHiddenAircraftCategories(categories: Set<String>) { write(HIDDEN_CATEGORIES, categories) }
  suspend fun setHiddenVesselCategories(categories: Set<String>) { write(HIDDEN_VESSEL_CATEGORIES, categories) }
  suspend fun setHiddenVesselStatuses(statuses: Set<String>) { write(HIDDEN_VESSEL_STATUSES, statuses) }

  private fun <T> read(key: Preferences.Key<T>, default: T): Flow<T> {
    return store.data.map { it[key] ?: default }
  }

  private suspend fun <T> write(key: Preferences.Key<T>, value: T) {
    store.edit { it[key] = value }
  }

  private companion object Keys {
    val AIS_API_KEY = stringPreferencesKey("ais_api_key")
    val ADSB_PROVIDER = stringPreferencesKey("adsb_provider")
    val TILE_STYLE = stringPreferencesKey("tile_style")
    val AIS_ENABLED = booleanPreferencesKey("ais_enabled")
    val TRAILS_ENABLED = booleanPreferencesKey("trails_enabled")
    val AVIATION_VISIBLE = booleanPreferencesKey("aviation_visible")
    val MARITIME_VISIBLE = booleanPreferencesKey("maritime_visible")
    val ADSB_POLL_MS = longPreferencesKey("adsb_poll_ms")
    val MARKER_SCALE = floatPreferencesKey("marker_scale")
    val MIN_ALT_M = intPreferencesKey("min_altitude_m")
    val MAX_ALT_M = intPreferencesKey("max_altitude_m")
    val HIDE_GROUNDED = booleanPreferencesKey("hide_grounded")
    val HIDDEN_CATEGORIES = stringSetPreferencesKey("hidden_categories")
    val HIDDEN_VESSEL_CATEGORIES = stringSetPreferencesKey("hidden_vessel_categories")
    val HIDDEN_VESSEL_STATUSES = stringSetPreferencesKey("hidden_vessel_statuses")
  }
}

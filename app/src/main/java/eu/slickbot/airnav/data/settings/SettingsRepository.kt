package eu.slickbot.airnav.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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

    private companion object {
        val KEY_ADSB_PROVIDER: Preferences.Key<String> = stringPreferencesKey("adsb_provider")
        val KEY_TILE_STYLE: Preferences.Key<String> = stringPreferencesKey("tile_style")
        val KEY_AIS_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("ais_enabled")
        val KEY_TRAILS_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("trails_enabled")
    }
}

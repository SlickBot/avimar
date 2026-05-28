package eu.slickbot.airnav.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import eu.slickbot.airnav.data.adsb.AdsbProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "airnav_settings")

class SettingsRepository(private val context: Context) {

    private val store get() = context.applicationContext.dataStore

    val adsbProvider: Flow<AdsbProvider> = store.data.map { prefs ->
        AdsbProvider.fromId(prefs[KEY_ADSB_PROVIDER])
    }

    suspend fun setAdsbProvider(provider: AdsbProvider) {
        store.edit { it[KEY_ADSB_PROVIDER] = provider.id }
    }

    private companion object {
        val KEY_ADSB_PROVIDER: Preferences.Key<String> = stringPreferencesKey("adsb_provider")
    }
}

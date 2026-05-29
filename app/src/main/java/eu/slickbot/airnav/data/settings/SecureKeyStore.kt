package eu.slickbot.airnav.data.settings

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.core.content.edit

private const val FILE = "airnav_secure"
private const val KEY_AIS = "ais_api_key"

class SecureKeyStore(context: Context) {

    private val prefs by lazy {
        val master = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            FILE,
            master,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    val aisApiKey: Flow<String?> = callbackFlow {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            if (key == KEY_AIS) trySend(sp.getString(KEY_AIS, null))
        }
        trySend(prefs.getString(KEY_AIS, null))
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun setAisApiKey(value: String?) {
        prefs.edit {
          if (value.isNullOrBlank()) remove(KEY_AIS) else putString(KEY_AIS, value.trim())
        }
    }
}

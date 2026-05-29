package eu.slickbot.airnav.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

private const val FILE = "airnav_secure"
private const val FALLBACK_FILE = "airnav_secure_fallback"
private const val KEY_AIS = "ais_api_key"

/**
 * Encrypted store for the AIS API key.
 *
 * EncryptedSharedPreferences (deprecated Jetpack Security) throws a hard
 * KeyStoreException ("Signature/MAC verification failed") when its keystore key
 * and the on-disk file get out of sync — which happens on reinstall, restore, or
 * key rotation, and would crash the app on startup. Every access is therefore
 * defensive: on corruption we wipe the encrypted file and recreate it, falling
 * back to a plain prefs file as a last resort so the app never crashes.
 */
@Suppress("DEPRECATION")
class SecureKeyStore(context: Context) {

  private val appContext = context.applicationContext
  private val prefs: SharedPreferences by lazy { createPrefs() }

  private fun buildEncrypted(): SharedPreferences {
    val master = MasterKey.Builder(appContext)
      .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
      .build()
    return EncryptedSharedPreferences.create(
      appContext,
      FILE,
      master,
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
  }

  private fun createPrefs(): SharedPreferences {
    return try {
      buildEncrypted()
    } catch (_: Exception) {
      // Corrupt keystore/file mismatch: drop the encrypted file and rebuild fresh.
      appContext.deleteSharedPreferences(FILE)
      runCatching { buildEncrypted() }.getOrElse {
        appContext.getSharedPreferences(FALLBACK_FILE, Context.MODE_PRIVATE)
      }
    }
  }

  val aisApiKey: Flow<String?> = callbackFlow {
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (key == KEY_AIS) trySend(readKey())
    }
    trySend(readKey())
    prefs.registerOnSharedPreferenceChangeListener(listener)
    awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
  }.distinctUntilChanged()

  private fun readKey(): String? {
    return runCatching { prefs.getString(KEY_AIS, null) }.getOrNull()
  }

  fun setAisApiKey(value: String?) {
    runCatching {
      prefs.edit {
        if (value.isNullOrBlank()) remove(KEY_AIS) else putString(KEY_AIS, value.trim())
      }
    }
  }
}

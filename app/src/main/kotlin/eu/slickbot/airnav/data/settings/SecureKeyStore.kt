package eu.slickbot.airnav.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

private const val FILE = "airnav_secure"
private const val KEY_AIS = "ais_api_key"

/**
 * Encrypted store for the AIS API key.
 *
 * The value is encrypted by [StringCipher] (Android Keystore-backed by default)
 * before it touches disk, so the backing SharedPreferences file only ever holds
 * ciphertext. A read that fails to decrypt — e.g. after a backup/restore rotates
 * the Keystore key out from under the stored blob — surfaces as null rather than
 * crashing the app.
 */
class SecureKeyStore(
  context: Context,
  private val cipher: StringCipher = KeystoreStringCipher(),
) {

  private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

  val aisApiKey: Flow<String?> = callbackFlow {
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (key == KEY_AIS) trySend(readKey())
    }
    trySend(readKey())
    prefs.registerOnSharedPreferenceChangeListener(listener)
    awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
  }.distinctUntilChanged()

  fun setAisApiKey(value: String?) {
    runCatching {
      prefs.edit {
        if (value.isNullOrBlank()) {
          remove(KEY_AIS)
        } else {
          putString(KEY_AIS, cipher.encrypt(value.trim()))
        }
      }
    }
  }

  private fun readKey(): String? {
    val stored = prefs.getString(KEY_AIS, null) ?: return null
    return cipher.decrypt(stored)
  }
}

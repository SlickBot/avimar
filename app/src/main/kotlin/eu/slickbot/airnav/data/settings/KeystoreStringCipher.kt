package eu.slickbot.airnav.data.settings

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "airnav_ais_key_v1"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_TAG_BITS = 128
private const val IV_BYTES = 12

/**
 * AES-256-GCM encryption backed by a key held in the Android Keystore. The raw
 * key material never leaves the Keystore; we only ask it to encrypt and decrypt.
 * Replaces the deprecated Jetpack Security EncryptedSharedPreferences.
 *
 * Stored form is Base64(IV ‖ ciphertext+tag); a fresh random IV is generated per
 * encryption, as GCM requires. decrypt() returns null on any failure, so a key
 * rotated out by backup/restore or a tampered blob yields no value instead of a
 * crash.
 */
class KeystoreStringCipher : StringCipher {

  override fun encrypt(plain: String): String {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
    val iv = cipher.iv
    val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
    return Base64.encodeToString(iv + ciphertext, Base64.NO_WRAP)
  }

  override fun decrypt(stored: String): String? {
    val key = getKey() ?: return null
    return runCatching {
      val combined = Base64.decode(stored, Base64.NO_WRAP)
      val iv = combined.copyOfRange(0, IV_BYTES)
      val ciphertext = combined.copyOfRange(IV_BYTES, combined.size)
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
      String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }.getOrNull()
  }

  private fun getOrCreateKey(): SecretKey {
    return getKey() ?: createKey()
  }

  private fun getKey(): SecretKey? {
    val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
    val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
    return entry?.secretKey
  }

  private fun createKey(): SecretKey {
    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
    val spec = KeyGenParameterSpec.Builder(
      KEY_ALIAS,
      KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
    )
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setKeySize(256)
      .build()
    generator.init(spec)
    return generator.generateKey()
  }
}

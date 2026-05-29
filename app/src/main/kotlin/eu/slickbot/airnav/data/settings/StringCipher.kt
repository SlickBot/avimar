package eu.slickbot.airnav.data.settings

/**
 * Symmetric string encryption. Implementations encrypt a value to an opaque,
 * persistable string and decrypt it back, returning null when the stored input
 * cannot be decrypted (wrong/rotated key, tampering, corruption).
 */
interface StringCipher {
  fun encrypt(plain: String): String
  fun decrypt(stored: String): String?
}

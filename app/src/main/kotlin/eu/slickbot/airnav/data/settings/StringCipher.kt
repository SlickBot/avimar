package eu.slickbot.airnav.data.settings

interface StringCipher {
  fun encrypt(plain: String): String
  fun decrypt(stored: String): String?
}

package eu.slickbot.avimar.data.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeystoreStringCipherTest {

  private val cipher = KeystoreStringCipher()

  @Test
  fun encryptThenDecryptReturnsOriginal() {
    val plain = "ais-secret-12345"
    val decrypted = cipher.decrypt(cipher.encrypt(plain))
    assertEquals(plain, decrypted)
  }

  @Test
  fun freshIvMakesCiphertextDifferEachTimeAndHideThePlaintext() {
    val plain = "ais-secret-12345"
    val first = cipher.encrypt(plain)
    val second = cipher.encrypt(plain)
    assertNotEquals(plain, first)
    assertNotEquals(first, second)
  }

  @Test
  fun garbageCiphertextDecryptsToNull() {
    assertNull(cipher.decrypt("not-valid-base64-or-ciphertext"))
  }
}

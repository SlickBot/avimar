package eu.slickbot.avimar.data.settings

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class SecureKeyStoreTest {

  private val fakeCipher = object : StringCipher {
    override fun encrypt(plain: String): String {
      return "enc:$plain"
    }
    override fun decrypt(stored: String): String? {
      return if (stored.startsWith("enc:")) stored.removePrefix("enc:") else null
    }
  }

  private fun context(): Context {
    return ApplicationProvider.getApplicationContext()
  }

  @Test
  fun `key round-trips through the cipher`() = runBlocking {
    val store = SecureKeyStore(context(), fakeCipher)
    assertNull(store.aisApiKey.first())

    store.setAisApiKey("secret-token")
    assertEquals("secret-token", store.aisApiKey.first())
  }

  @Test
  fun `stored value is ciphertext, not plaintext`() = runBlocking {
    val context = context()
    SecureKeyStore(context, fakeCipher).setAisApiKey("secret-token")

    val raw = context.getSharedPreferences("avimar_secure", Context.MODE_PRIVATE)
      .getString("ais_api_key", null)
    assertEquals("enc:secret-token", raw)
  }

  @Test
  fun `blank value clears the stored key`() = runBlocking {
    val store = SecureKeyStore(context(), fakeCipher)
    store.setAisApiKey("secret-token")
    store.setAisApiKey("")
    assertNull(store.aisApiKey.first())
  }

  @Test
  fun `undecryptable stored value surfaces as null, never crashes`() = runBlocking {
    val context = context()
    context.getSharedPreferences("avimar_secure", Context.MODE_PRIVATE)
      .edit().putString("ais_api_key", "not-ciphertext").commit()

    val store = SecureKeyStore(context, fakeCipher)
    assertNull(store.aisApiKey.first())
  }
}

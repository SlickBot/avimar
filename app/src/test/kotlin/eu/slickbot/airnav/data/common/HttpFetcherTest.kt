package eu.slickbot.airnav.data.common

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class HttpFetcherTest {
  private lateinit var server: MockWebServer
  private lateinit var fetcher: JsonTextFetcher

  @Before
  fun setUp() {
    server = MockWebServer()
    server.start()
    fetcher = JsonTextFetcher(client = OkHttpClient())
  }

  @After
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun `fetch returns body on 200`() {
    server.enqueue(MockResponse().setBody("""{"ok":true}""").setResponseCode(200))

    val body = fetcher.fetch(
      tag = "test",
      baseUrl = server.url("/").toString().trimEnd('/'),
      path = "/v1/x",
    )

    assertEquals("""{"ok":true}""", body)
    val req = server.takeRequest()
    assertEquals("/v1/x", req.path)
    assertEquals("application/json", req.getHeader("Accept"))
  }

  @Test
  fun `fetch throws FetchException with status on 404`() {
    server.enqueue(MockResponse().setResponseCode(404).setBody("nope"))

    try {
      fetcher.fetch(
        tag = "test",
        baseUrl = server.url("/").toString().trimEnd('/'),
        path = "/v1/x",
      )
      fail("expected FetchException")
    } catch (e: FetchException) {
      assertEquals(404, e.statusCode)
    }
  }

  @Test
  fun `fetch throws FetchException with null status on transport failure`() {
    val baseUrl = server.url("/").toString().trimEnd('/')
    server.shutdown()  // force connect failure

    try {
      fetcher.fetch(tag = "test", baseUrl = baseUrl, path = "/v1/x")
      fail("expected FetchException")
    } catch (e: FetchException) {
      assertNull(e.statusCode)
    }
  }

  @Test
  fun `fetchUrl splits absolute URL into baseUrl and path with query preserved`() {
    server.enqueue(MockResponse().setBody("hi").setResponseCode(200))

    val body = fetcher.fetchUrl(
      tag = "test",
      url = server.url("/foo/bar?x=1&y=2").toString(),
    )

    assertEquals("hi", body)
    val req = server.takeRequest()
    assertEquals("/foo/bar?x=1&y=2", req.path)
  }

  @Test
  fun `fetchUrlOrNull returns null on FetchException`() {
    server.enqueue(MockResponse().setResponseCode(500))

    val body = fetcher.fetchUrlOrNull(tag = "test", url = server.url("/x").toString())

    assertNull(body)
  }

  @Test
  fun `baseUrl trailing slash and path missing leading slash both work`() {
    server.enqueue(MockResponse().setBody("ok").setResponseCode(200))

    val body = fetcher.fetch(
      tag = "test",
      baseUrl = server.url("/").toString(),  // trailing /
      path = "v1/x",                          // no leading /
    )

    assertEquals("ok", body)
    assertEquals("/v1/x", server.takeRequest().path)
  }

  @Test
  fun `HttpTextFetcher sends text accept header`() {
    server.enqueue(MockResponse().setBody("<html/>").setResponseCode(200))
    val text = HttpTextFetcher(client = OkHttpClient())

    text.fetch(
      tag = "test",
      baseUrl = server.url("/").toString().trimEnd('/'),
      path = "/page",
    )

    val req = server.takeRequest()
    assertTrue(req.getHeader("Accept")!!.contains("text/html"))
  }
}

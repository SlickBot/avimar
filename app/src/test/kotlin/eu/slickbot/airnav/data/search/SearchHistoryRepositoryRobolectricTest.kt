package eu.slickbot.airnav.data.search

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class SearchHistoryRepositoryRobolectricTest {

  private lateinit var repo: SearchHistoryRepository

  private fun plane(id: String, title: String = id) =
    RecentEntity(RecentEntity.Kind.PLANE, id, title, subtitle = null, lat = 1.0, lon = 2.0)

  @Before
  fun setUp() {
    repo = SearchHistoryRepository(ApplicationProvider.getApplicationContext(), Json)
  }

  @After
  fun tearDown() = runBlocking {
    repo.clear()
  }

  @Test
  fun `starts empty`() = runBlocking {
    assertTrue(repo.recentEntities.first().isEmpty())
  }

  @Test
  fun `add prepends most-recent-first and round-trips`() = runBlocking {
    repo.add(plane("a"))
    repo.add(plane("b"))
    val list = repo.recentEntities.first()
    assertEquals(listOf("b", "a"), list.map { it.id })
    assertEquals(RecentEntity.Kind.PLANE, list.first().kind)
  }

  @Test
  fun `re-adding same kind+id moves to front and refreshes snapshot, no duplicate`() = runBlocking {
    repo.add(plane("a", title = "old"))
    repo.add(plane("b"))
    repo.add(plane("A", title = "new"))
    val list = repo.recentEntities.first()
    assertEquals(listOf("A", "b"), list.map { it.id })
    assertEquals("new", list.first().title)
  }

  @Test
  fun `caps at 8 dropping the oldest`() = runBlocking {
    repeat(10) { repo.add(plane("p$it")) }
    val list = repo.recentEntities.first()
    assertEquals(8, list.size)
    assertEquals("p9", list.first().id)
    assertEquals("p2", list.last().id)
  }

  @Test
  fun `remove drops by kind and id`() = runBlocking {
    repo.add(plane("a"))
    repo.add(plane("b"))
    repo.remove(plane("a"))
    assertEquals(listOf("b"), repo.recentEntities.first().map { it.id })
  }
}

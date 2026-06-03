package eu.slickbot.avimar.ui.map

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LoadableTest {
  @Test
  fun `emits loading then ready with value`() = runTest {
    val emissions = loadableFetch { "x" }.toList()
    assertEquals(listOf(Loadable.Loading, Loadable.Ready("x")), emissions)
  }

  @Test
  fun `emits ready null when fetch returns null`() = runTest {
    val emissions = loadableFetch<String> { null }.toList()
    assertEquals(listOf(Loadable.Loading, Loadable.Ready<String>(null)), emissions)
  }

  @Test
  fun `emits ready null when fetch throws`() = runTest {
    val emissions = loadableFetch<String> { error("boom") }.toList()
    assertEquals(listOf(Loadable.Loading, Loadable.Ready<String>(null)), emissions)
  }
}

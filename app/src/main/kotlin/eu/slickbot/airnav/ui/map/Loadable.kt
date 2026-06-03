package eu.slickbot.airnav.ui.map

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * State of an async fetch: still loading, or done with a value (null on miss or
 * failure). Lets the detail sheet show a loader until the fetch resolves.
 */
sealed interface Loadable<out T> {
  data object Loading : Loadable<Nothing>
  data class Ready<out T>(val value: T?) : Loadable<T>
}

/** Emits [Loadable.Loading], then [Loadable.Ready] with [fetch]'s result (null if it throws). */
fun <T> loadableFetch(fetch: suspend () -> T?): Flow<Loadable<T>> {
  return flow {
    emit(Loadable.Loading)
    emit(Loadable.Ready(runCatching { fetch() }.getOrNull()))
  }
}

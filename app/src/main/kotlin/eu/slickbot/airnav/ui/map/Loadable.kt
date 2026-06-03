package eu.slickbot.airnav.ui.map

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed interface Loadable<out T> {
  data object Loading : Loadable<Nothing>
  data class Ready<out T>(val value: T?) : Loadable<T>
}

fun <T> loadableFetch(fetch: suspend () -> T?): Flow<Loadable<T>> {
  return flow {
    emit(Loadable.Loading)
    emit(Loadable.Ready(runCatching { fetch() }.getOrNull()))
  }
}

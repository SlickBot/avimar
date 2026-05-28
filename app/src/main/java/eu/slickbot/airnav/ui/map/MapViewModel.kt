package eu.slickbot.airnav.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.slickbot.airnav.data.adsb.Aircraft
import eu.slickbot.airnav.data.adsb.AdsbProvider
import eu.slickbot.airnav.data.adsb.AdsbRepository
import kotlin.math.ceil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModel(
    private val repository: AdsbRepository = AdsbRepository(),
) : ViewModel() {

    private val _viewport = MutableStateFlow<Viewport?>(null)
    private val _aircraft = MutableStateFlow<List<Aircraft>>(emptyList())
    val aircraft: StateFlow<List<Aircraft>> = _aircraft.asStateFlow()

    private val provider = AdsbProvider.AdsbFi
    private val pollIntervalMs = 5_000L

    init {
        viewModelScope.launch {
            _viewport
                .filterNotNull()
                .debounce(300)
                .flatMapLatest { vp ->
                    flow {
                        while (true) {
                            emit(vp)
                            delay(pollIntervalMs)
                        }
                    }
                }
                .collect { vp -> fetch(vp) }
        }
    }

    fun onViewportChanged(viewport: Viewport) {
        _viewport.value = viewport
    }

    private suspend fun fetch(viewport: Viewport) {
        val radius = ceil(viewport.radiusNm).toInt().coerceAtLeast(1)
        runCatching {
            repository.fetch(provider, viewport.centerLat, viewport.centerLon, radius)
        }.onSuccess { list ->
            _aircraft.value = list
        }.onFailure { t ->
            Log.w(TAG, "ADS-B fetch failed: ${t.message}")
        }
    }

    companion object {
        private const val TAG = "MapViewModel"
    }
}

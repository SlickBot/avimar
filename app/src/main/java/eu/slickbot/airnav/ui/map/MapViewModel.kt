package eu.slickbot.airnav.ui.map

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import eu.slickbot.airnav.data.adsb.Aircraft
import eu.slickbot.airnav.data.adsb.AdsbProvider
import eu.slickbot.airnav.data.adsb.AdsbRepository
import eu.slickbot.airnav.data.settings.SettingsRepository
import kotlin.math.ceil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModel(
    private val adsbRepo: AdsbRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _viewport = MutableStateFlow<Viewport?>(null)
    private val _aircraft = MutableStateFlow<List<Aircraft>>(emptyList())
    val aircraft: StateFlow<List<Aircraft>> = _aircraft.asStateFlow()

    val provider: StateFlow<AdsbProvider> = settings.adsbProvider.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AdsbProvider.AdsbFi,
    )

    init {
        viewModelScope.launch {
            combine(
                _viewport.filterNotNull().debounce(300),
                provider,
            ) { vp, p -> vp to p }
                .flatMapLatest { (vp, p) ->
                    flow {
                        while (true) {
                            emit(vp to p)
                            delay(p.minPollIntervalMs)
                        }
                    }
                }
                .collect { (vp, p) -> fetch(p, vp) }
        }
    }

    fun onViewportChanged(viewport: Viewport) {
        _viewport.value = viewport
    }

    fun selectProvider(provider: AdsbProvider) {
        viewModelScope.launch { settings.setAdsbProvider(provider) }
    }

    private suspend fun fetch(provider: AdsbProvider, viewport: Viewport) {
        val radius = ceil(viewport.radiusNm).toInt().coerceAtLeast(1)
        runCatching {
            adsbRepo.fetch(provider, viewport.centerLat, viewport.centerLon, radius)
        }.onSuccess { list ->
            _aircraft.value = list
        }.onFailure { t ->
            Log.w(TAG, "ADS-B fetch failed (${provider.id}): ${t.message}")
        }
    }

    class Factory(private val appContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MapViewModel::class.java))
            return MapViewModel(
                adsbRepo = AdsbRepository(),
                settings = SettingsRepository(appContext),
            ) as T
        }
    }

    companion object {
        private const val TAG = "MapViewModel"
    }
}

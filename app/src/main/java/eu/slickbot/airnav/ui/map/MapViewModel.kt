package eu.slickbot.airnav.ui.map

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import eu.slickbot.airnav.data.adsb.Aircraft
import eu.slickbot.airnav.data.adsb.AdsbProvider
import eu.slickbot.airnav.data.adsb.AdsbRepository
import eu.slickbot.airnav.data.ais.AisRepository
import eu.slickbot.airnav.data.ais.Vessel
import eu.slickbot.airnav.data.settings.SecureKeyStore
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
    private val aisRepo: AisRepository,
    private val settings: SettingsRepository,
    private val secureStore: SecureKeyStore,
) : ViewModel() {

    private val _viewport = MutableStateFlow<Viewport?>(null)
    private val _aircraft = MutableStateFlow<List<Aircraft>>(emptyList())
    val aircraft: StateFlow<List<Aircraft>> = _aircraft.asStateFlow()
    val vessels: StateFlow<List<Vessel>> = aisRepo.vessels

    val provider: StateFlow<AdsbProvider> = settings.adsbProvider.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AdsbProvider.AdsbFi,
    )

    val aisApiKey: StateFlow<String?> = secureStore.aisApiKey.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    init {
        // ADS-B polling loop.
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
                .collect { (vp, p) -> fetchAdsb(p, vp) }
        }

        // AIS lifecycle: connect when key is set, disconnect when cleared.
        viewModelScope.launch {
            combine(aisApiKey, _viewport.filterNotNull().debounce(400)) { key, vp -> key to vp }
                .collect { (key, vp) ->
                    val bbox = vp.boundingBox()
                    if (key.isNullOrBlank()) {
                        aisRepo.stop()
                    } else {
                        aisRepo.start(key, bbox)
                    }
                }
        }
    }

    fun onViewportChanged(viewport: Viewport) {
        _viewport.value = viewport
    }

    fun selectProvider(provider: AdsbProvider) {
        viewModelScope.launch { settings.setAdsbProvider(provider) }
    }

    fun setAisApiKey(key: String?) {
        secureStore.setAisApiKey(key)
    }

    private suspend fun fetchAdsb(provider: AdsbProvider, viewport: Viewport) {
        val radius = ceil(viewport.radiusNm).toInt().coerceAtLeast(1)
        runCatching {
            adsbRepo.fetch(provider, viewport.centerLat, viewport.centerLon, radius)
        }.onSuccess { list ->
            _aircraft.value = list
        }.onFailure { t ->
            Log.w(TAG, "ADS-B fetch failed (${provider.id}): ${t.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        aisRepo.shutdown()
    }

    class Factory(private val appContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MapViewModel::class.java))
            return MapViewModel(
                adsbRepo = AdsbRepository(),
                aisRepo = AisRepository(),
                settings = SettingsRepository(appContext),
                secureStore = SecureKeyStore(appContext),
            ) as T
        }
    }

    companion object {
        private const val TAG = "MapViewModel"
    }
}

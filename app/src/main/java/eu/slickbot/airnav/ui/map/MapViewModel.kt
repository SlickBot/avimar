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

sealed interface MapSelection {
    data class Plane(val aircraft: Aircraft) : MapSelection
    data class Ship(val vessel: Vessel) : MapSelection
}

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

    val tileStyle: StateFlow<TileStyle> = settings.tileStyle.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TileStyle.CartoDark,
    )

    val aisEnabled: StateFlow<Boolean> = settings.aisEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true,
    )

    val trailsEnabled: StateFlow<Boolean> = settings.trailsEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true,
    )

    val aisApiKey: StateFlow<String?> = secureStore.aisApiKey.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    private val _selectedAircraftHex = MutableStateFlow<String?>(null)
    private val _selectedVesselMmsi = MutableStateFlow<Long?>(null)

    val selection: StateFlow<MapSelection?> = combine(
        _selectedAircraftHex,
        _selectedVesselMmsi,
        _aircraft,
        aisRepo.vessels,
    ) { hex, mmsi, planes, ships ->
        when {
            hex != null -> planes.firstOrNull { it.hex == hex }?.let { MapSelection.Plane(it) }
            mmsi != null -> ships.firstOrNull { it.mmsi == mmsi }?.let { MapSelection.Ship(it) }
            else -> null
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

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

        // AIS lifecycle: connect when (key set && AIS enabled), disconnect otherwise.
        viewModelScope.launch {
            combine(
                aisApiKey,
                aisEnabled,
                _viewport.filterNotNull().debounce(400),
            ) { key, on, vp -> Triple(key, on, vp) }
                .collect { (key, on, vp) ->
                    if (key.isNullOrBlank() || !on) {
                        aisRepo.stop()
                    } else {
                        aisRepo.start(key, vp.boundingBox())
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

    fun selectTileStyle(style: TileStyle) {
        viewModelScope.launch { settings.setTileStyle(style) }
    }

    fun setAisEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setAisEnabled(enabled) }
    }

    fun setTrailsEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setTrailsEnabled(enabled) }
    }

    fun setAisApiKey(key: String?) {
        secureStore.setAisApiKey(key)
    }

    fun selectAircraft(hex: String) {
        _selectedVesselMmsi.value = null
        _selectedAircraftHex.value = hex
    }

    fun selectVessel(mmsi: Long) {
        _selectedAircraftHex.value = null
        _selectedVesselMmsi.value = mmsi
    }

    fun clearSelection() {
        _selectedAircraftHex.value = null
        _selectedVesselMmsi.value = null
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

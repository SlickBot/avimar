package eu.slickbot.airnav.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import eu.slickbot.airnav.AppContainer
import eu.slickbot.airnav.data.adsb.Aircraft
import eu.slickbot.airnav.data.adsb.AdsbProvider
import eu.slickbot.airnav.data.adsb.AdsbRepository
import eu.slickbot.airnav.data.adsb.AircraftDetails
import eu.slickbot.airnav.data.adsb.AircraftEnricher
import eu.slickbot.airnav.data.ais.AisConnectionState
import eu.slickbot.airnav.data.ais.AisRepository
import eu.slickbot.airnav.data.ais.Vessel
import eu.slickbot.airnav.data.ais.info.VesselDetails
import eu.slickbot.airnav.data.ais.info.VesselEnricher
import eu.slickbot.airnav.data.location.LatLon
import eu.slickbot.airnav.data.location.LocationProvider
import eu.slickbot.airnav.data.settings.SecureKeyStore
import eu.slickbot.airnav.data.settings.SettingsRepository
import kotlin.math.ceil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MapSelection {
    data class Plane(val aircraft: Aircraft) : MapSelection
    data class Ship(val vessel: Vessel) : MapSelection
}

data class CenterCommand(val lat: Double, val lon: Double, val zoom: Double?)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MapViewModel(
    private val adsbRepo: AdsbRepository,
    private val aisRepo: AisRepository,
    private val settings: SettingsRepository,
    private val secureStore: SecureKeyStore,
    private val locationProvider: LocationProvider,
    private val enricher: AircraftEnricher,
    private val vesselEnricher: VesselEnricher,
) : ViewModel() {

    private val _viewport = MutableStateFlow<Viewport?>(null)
    private val _aircraft = MutableStateFlow<List<Aircraft>>(emptyList())
    val aircraft: StateFlow<List<Aircraft>> = _aircraft.asStateFlow()
    val vessels: StateFlow<List<Vessel>> = aisRepo.vessels
    val aisConnection: StateFlow<AisConnectionState> = aisRepo.connectionState

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

    val aviationVisible: StateFlow<Boolean> = settings.aviationVisible.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true,
    )

    val maritimeVisible: StateFlow<Boolean> = settings.maritimeVisible.stateIn(
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
    private val _following = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _following.asStateFlow()

    val onboardingSeen: StateFlow<Boolean> = settings.onboardingSeen.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true, // assume seen until DataStore loads, so we don't flash the dialog
    )

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

    val planeEnrichment: StateFlow<AircraftDetails?> = _selectedAircraftHex
        .flatMapLatest { hex ->
            if (hex == null) flowOf<AircraftDetails?>(null)
            else flow {
                emit(null)
                emit(runCatching { enricher.lookup(hex) }.getOrNull())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val shipEnrichment: StateFlow<VesselDetails?> = _selectedVesselMmsi
        .flatMapLatest { mmsi ->
            if (mmsi == null) {
                flowOf<VesselDetails?>(null)
            } else {
                val imo = aisRepo.vessels.value.firstOrNull { it.mmsi == mmsi }?.imo
                flow {
                    emit(null)
                    emit(runCatching { vesselEnricher.lookup(mmsi, imo) }.getOrNull())
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _centerCommands = MutableSharedFlow<CenterCommand>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val centerCommands: SharedFlow<CenterCommand> = _centerCommands.asSharedFlow()

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

        // AIS lifecycle.
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

    fun setAviationVisible(visible: Boolean) {
        viewModelScope.launch { settings.setAviationVisible(visible) }
    }

    fun setMaritimeVisible(visible: Boolean) {
        viewModelScope.launch { settings.setMaritimeVisible(visible) }
    }

    fun setAisApiKey(key: String?) {
        secureStore.setAisApiKey(key)
    }

    fun markOnboardingSeen() {
        viewModelScope.launch { settings.setOnboardingSeen(true) }
    }

    fun selectAircraft(hex: String) {
        _selectedVesselMmsi.value = null
        _selectedAircraftHex.value = hex
        _following.value = false
    }

    fun selectVessel(mmsi: Long) {
        _selectedAircraftHex.value = null
        _selectedVesselMmsi.value = mmsi
        _following.value = false
    }

    fun clearSelection() {
        _selectedAircraftHex.value = null
        _selectedVesselMmsi.value = null
        _following.value = false
    }

    fun setFollowing(on: Boolean) {
        _following.value = on
    }

    fun centerOnUser() {
        val fix: LatLon = locationProvider.lastKnown() ?: return
        _centerCommands.tryEmit(CenterCommand(fix.lat, fix.lon, zoom = 9.0))
    }

    fun centerOn(lat: Double, lon: Double, zoom: Double? = null) {
        _centerCommands.tryEmit(CenterCommand(lat, lon, zoom))
    }

    fun hasLocationPermission(): Boolean = locationProvider.hasPermission()

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

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MapViewModel::class.java))
            return MapViewModel(
                adsbRepo = container.adsbRepository,
                aisRepo = container.aisRepository,
                settings = container.settingsRepository,
                secureStore = container.secureKeyStore,
                locationProvider = container.locationProvider,
                enricher = container.aircraftEnricher,
                vesselEnricher = container.vesselEnricher,
            ) as T
        }
    }

    companion object {
        private const val TAG = "MapViewModel"
    }
}

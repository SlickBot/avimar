package eu.slickbot.airnav.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.slickbot.airnav.data.adsb.AdsbProvider
import eu.slickbot.airnav.data.adsb.AdsbRepository
import eu.slickbot.airnav.data.adsb.Aircraft
import eu.slickbot.airnav.data.adsb.info.AircraftInfo
import eu.slickbot.airnav.data.adsb.info.AircraftInfoProvider
import eu.slickbot.airnav.data.ais.AisConnectionState
import eu.slickbot.airnav.data.ais.AisRepository
import eu.slickbot.airnav.data.ais.Vessel
import eu.slickbot.airnav.data.ais.info.VesselDetails
import eu.slickbot.airnav.data.ais.info.VesselEnricher
import eu.slickbot.airnav.data.ais.translate.AisStreamHealth
import eu.slickbot.airnav.data.common.NetworkTuning
import eu.slickbot.airnav.data.location.LatLon
import eu.slickbot.airnav.data.location.LocationProvider
import eu.slickbot.airnav.data.search.RecentEntity
import eu.slickbot.airnav.data.search.SearchHistoryRepository
import eu.slickbot.airnav.data.settings.SecureKeyStore
import eu.slickbot.airnav.data.settings.SettingsRepository
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
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.ceil

sealed interface MapSelection {
  data class Plane(val aircraft: Aircraft) : MapSelection
  data class Ship(val vessel: Vessel) : MapSelection
}

data class CenterCommand(val lat: Double, val lon: Double, val zoom: Double?)

@Suppress("ConvertLongToDuration")
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MapViewModel(
  private val adsbRepo: AdsbRepository,
  private val aisRepo: AisRepository,
  private val settings: SettingsRepository,
  private val secureStore: SecureKeyStore,
  private val locationProvider: LocationProvider,
  private val aircraftInfoProvider: AircraftInfoProvider,
  private val vesselEnricher: VesselEnricher,
  private val searchHistory: SearchHistoryRepository,
) : ViewModel() {

  companion object {
    private const val TAG = "MapViewModel"
  }

  private val _viewport = MutableStateFlow<Viewport?>(null)
  private val _aircraft = MutableStateFlow<List<Aircraft>>(emptyList())

  private val aircraftFilter: StateFlow<AircraftFilter> = combine(
    settings.minAltitudeM,
    settings.maxAltitudeM,
    settings.hideGrounded,
    settings.hiddenAircraftCategories,
  ) { min, max, hideGrounded, hidden ->
    AircraftFilter(
      minAltitudeM = min,
      maxAltitudeM = max,
      hideGrounded = hideGrounded,
      hiddenCategoryCodes = hidden,
    )
  }.stateIn(viewModelScope, SharingStarted.Eagerly, AircraftFilter.NONE)

  /** Aircraft as drawn on the map: the latest fetch with the user's filters applied. */
  val aircraft: StateFlow<List<Aircraft>> = combine(_aircraft, aircraftFilter) { list, filter ->
    filter.apply(list)
  }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val markerScale: StateFlow<Float> = settings.markerScale.stateIn(
    scope = viewModelScope,
    started = SharingStarted.Eagerly,
    initialValue = 1.0f,
  )

  val hiddenAircraftCategories: StateFlow<Set<String>> = settings.hiddenAircraftCategories.stateIn(
    viewModelScope, SharingStarted.Eagerly, emptySet(),
  )
  val hiddenVesselCategories: StateFlow<Set<String>> = settings.hiddenVesselCategories.stateIn(
    viewModelScope, SharingStarted.Eagerly, emptySet(),
  )
  val hiddenVesselStatuses: StateFlow<Set<String>> = settings.hiddenVesselStatuses.stateIn(
    viewModelScope, SharingStarted.Eagerly, emptySet(),
  )

  private val vesselFilter: StateFlow<VesselFilter> = combine(
    settings.hiddenVesselCategories,
    settings.hiddenVesselStatuses,
  ) { categories, statuses ->
    VesselFilter(hiddenCategoryCodes = categories, hiddenStatusCodes = statuses)
  }.stateIn(viewModelScope, SharingStarted.Eagerly, VesselFilter.NONE)

  /** Vessels as drawn on the map: the AIS feed with the user's filters applied. */
  val vessels: StateFlow<List<Vessel>> = combine(aisRepo.vessels, vesselFilter) { list, filter ->
    filter.apply(list)
  }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
  val aisConnection: StateFlow<AisConnectionState> = aisRepo.connectionState

  val aisHealth: StateFlow<AisStreamHealth?> = flow {
    while (true) {
      emit(aisRepo.health())
      delay(2_000L)
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

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

  // Settings sheet reads ~12 scalars; bundling lets the sheet take one snapshot
  // instead of a dozen `by collectAsStateWithLifecycle()` lines at the call site.
  val settingsState: StateFlow<SettingsState> = combine(
    secureStore.aisApiKey,
    settings.aisEnabled,
    settings.trailsEnabled,
    settings.aviationVisible,
    settings.maritimeVisible,
    settings.hideGrounded,
    settings.markerScale,
    settings.adsbPollMs,
    settings.adsbTimeoutSec,
    settings.minAltitudeM,
    settings.maxAltitudeM,
    settings.hiddenAircraftCategories,
    settings.hiddenVesselCategories,
    settings.hiddenVesselStatuses,
  ) { values ->
    @Suppress("UNCHECKED_CAST")
    SettingsState(
      aisApiKey = values[0] as String?,
      aisEnabled = values[1] as Boolean,
      trailsEnabled = values[2] as Boolean,
      aviationVisible = values[3] as Boolean,
      maritimeVisible = values[4] as Boolean,
      hideGrounded = values[5] as Boolean,
      markerScale = values[6] as Float,
      adsbPollMs = values[7] as Long,
      adsbTimeoutSec = values[8] as Int,
      minAltitudeM = values[9] as Int?,
      maxAltitudeM = values[10] as Int?,
      hiddenAircraftCategories = values[11] as Set<String>,
      hiddenVesselCategories = values[12] as Set<String>,
      hiddenVesselStatuses = values[13] as Set<String>,
    )
  }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState.Empty)

  val settingsActions: SettingsActions = object : SettingsActions {
    override fun setAisApiKey(value: String?) = this@MapViewModel.setAisApiKey(value)
    override fun setAisEnabled(enabled: Boolean) = this@MapViewModel.setAisEnabled(enabled)
    override fun setTrailsEnabled(enabled: Boolean) = this@MapViewModel.setTrailsEnabled(enabled)
    override fun setAviationVisible(visible: Boolean) = this@MapViewModel.setAviationVisible(visible)
    override fun setMaritimeVisible(visible: Boolean) = this@MapViewModel.setMaritimeVisible(visible)
    override fun setHideGrounded(hide: Boolean) = this@MapViewModel.setHideGrounded(hide)
    override fun setMarkerScale(scale: Float) = this@MapViewModel.setMarkerScale(scale)
    override fun setAdsbPollMs(ms: Long) = this@MapViewModel.setAdsbPollMs(ms)
    override fun setAdsbTimeoutSec(seconds: Int) = this@MapViewModel.setAdsbTimeoutSec(seconds)
    override fun setMinAltitudeM(meters: Int?) = this@MapViewModel.setMinAltitudeM(meters)
    override fun setMaxAltitudeM(meters: Int?) = this@MapViewModel.setMaxAltitudeM(meters)
    override fun toggleHiddenAircraftCategory(code: String) {
      val current = settingsState.value.hiddenAircraftCategories
      val next = if (code in current) current - code else current + code
      setHiddenAircraftCategories(next)
    }
    override fun toggleHiddenVesselCategory(code: String) {
      val current = settingsState.value.hiddenVesselCategories
      val next = if (code in current) current - code else current + code
      setHiddenVesselCategories(next)
    }
    override fun toggleHiddenVesselStatus(code: String) {
      val current = settingsState.value.hiddenVesselStatuses
      val next = if (code in current) current - code else current + code
      setHiddenVesselStatuses(next)
    }
  }

  private val _selectedAircraftHex = MutableStateFlow<String?>(null)
  private val _selectedVesselMmsi = MutableStateFlow<Long?>(null)
  private val _following = MutableStateFlow(false)
  val isFollowing: StateFlow<Boolean> = _following.asStateFlow()

  val selection: StateFlow<MapSelection?> = combine(
    _selectedAircraftHex,
    _selectedVesselMmsi,
    _aircraft,
    aisRepo.vessels,
  ) { hex, mmsi, planes, ships ->
    val active = hex != null || mmsi != null
    val resolved = when {
      hex != null -> planes.firstOrNull { it.hex == hex }?.let { MapSelection.Plane(it) }
      mmsi != null -> ships.firstOrNull { it.mmsi == mmsi }?.let { MapSelection.Ship(it) }
      else -> null
    }
    active to resolved
  }
    // Sticky selection: once a vehicle is picked, hold its last-known snapshot when
    // it drops out of the fetched set (panned away) instead of going null. Panning
    // therefore neither closes nor reopens the sheet; only an explicit deselect
    // (both ids cleared) does. Fresh data replaces the snapshot when it returns.
    .runningFold<Pair<Boolean, MapSelection?>, MapSelection?>(null) { prev, (active, resolved) ->
      when {
        !active -> null
        resolved != null -> resolved
        else -> prev
      }
    }
    .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  val recentEntities: StateFlow<List<RecentEntity>> = searchHistory.recentEntities.stateIn(
    scope = viewModelScope,
    started = SharingStarted.Eagerly,
    initialValue = emptyList(),
  )

  val planeEnrichment: StateFlow<Loadable<AircraftInfo>> = _selectedAircraftHex
    .flatMapLatest { hex ->
      if (hex == null) {
        flowOf(Loadable.Ready<AircraftInfo>(null))
      } else {
        val current = _aircraft.value.firstOrNull { it.hex == hex }
        loadableFetch {
          aircraftInfoProvider.fetch(
            icao = hex,
            callsign = current?.callsign,
            registration = current?.registration,
            typeDesignator = current?.type,
            operator = null,
            description = null,
          )
        }
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Loadable.Loading)

  val shipEnrichment: StateFlow<Loadable<VesselDetails>> = _selectedVesselMmsi
    .flatMapLatest { mmsi ->
      if (mmsi == null) {
        flowOf(Loadable.Ready<VesselDetails>(null))
      } else {
        val imo = aisRepo.vessels.value.firstOrNull { it.mmsi == mmsi }?.imo
        loadableFetch { vesselEnricher.lookup(mmsi, imo) }
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Loadable.Loading)

  private val _centerCommands = MutableSharedFlow<CenterCommand>(
    extraBufferCapacity = 4,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )
  val centerCommands: SharedFlow<CenterCommand> = _centerCommands.asSharedFlow()

  init {
    // Apply the configurable request timeout to the shared HTTP fetchers.
    viewModelScope.launch {
      settings.adsbTimeoutSec.collect { seconds -> NetworkTuning.callTimeoutMs = seconds * 1000L }
    }

    // ADS-B polling loop. Uses the poll-interval override when set, else the
    // provider default; emits immediately so the first fetch fires as soon as a
    // viewport is known (and on every pan / provider switch).
    viewModelScope.launch {
      combine(
        _viewport.filterNotNull().debounce(300),
        provider,
        settings.adsbPollMs,
      ) { vp, p, pollMs -> Triple(vp, p, pollMs) }
        .flatMapLatest { (vp, p, pollMs) ->
          val interval = if (pollMs > 0L) pollMs else p.minPollIntervalMs
          // Sequential: each fetch fully completes — or is cancelled by a newer
          // viewport/provider via flatMapLatest — before the next is scheduled.
          // No overlap, and the interval is measured from completion, not a fixed
          // wall-clock timer.
          flow {
            while (true) {
              fetchAdsb(p, vp)
              emit(Unit)
              delay(interval)
            }
          }
        }
        .collect { }
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

  fun selectAircraft(hex: String) {
    _selectedVesselMmsi.value = null
    _selectedAircraftHex.value = hex
    // Selecting a vehicle starts following it; a manual pan, close, or the chip stops it.
    _following.value = true
    _aircraft.value.firstOrNull { it.hex == hex }?.let { recordRecent(RecentEntity.fromAircraft(it)) }
  }

  fun selectVessel(mmsi: Long) {
    _selectedAircraftHex.value = null
    _selectedVesselMmsi.value = mmsi
    _following.value = true
    aisRepo.vessels.value.firstOrNull { it.mmsi == mmsi }?.let { recordRecent(RecentEntity.fromVessel(it)) }
  }

  private fun recordRecent(entity: RecentEntity) {
    viewModelScope.launch { searchHistory.add(entity) }
  }

  fun removeRecentEntity(entity: RecentEntity) {
    viewModelScope.launch { searchHistory.remove(entity) }
  }

  fun clearSelection() {
    _selectedAircraftHex.value = null
    _selectedVesselMmsi.value = null
    _following.value = false
  }

  fun setFollowing(on: Boolean) {
    _following.value = on
  }

  /** A manual map gesture takes camera control, so it disengages follow. */
  fun onMapGesture() {
    if (_following.value) {
      _following.value = false
    }
  }

  fun centerOnUser() {
    val fix: LatLon = locationProvider.lastKnown() ?: return
    _centerCommands.tryEmit(CenterCommand(fix.lat, fix.lon, zoom = 9.0))
  }

  fun centerOn(lat: Double, lon: Double, zoom: Double? = null) {
    _centerCommands.tryEmit(CenterCommand(lat, lon, zoom))
  }

  fun hasLocationPermission(): Boolean {
    return locationProvider.hasPermission()
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

  // ADS-B filter / fetch-tuning setters.

  fun setMarkerScale(scale: Float) {
    viewModelScope.launch { settings.setMarkerScale(scale) }
  }

  fun setAdsbPollMs(ms: Long) {
    viewModelScope.launch { settings.setAdsbPollMs(ms) }
  }

  fun setAdsbTimeoutSec(seconds: Int) {
    viewModelScope.launch { settings.setAdsbTimeoutSec(seconds) }
  }

  fun setMinAltitudeM(meters: Int?) {
    viewModelScope.launch { settings.setMinAltitudeM(meters) }
  }

  fun setMaxAltitudeM(meters: Int?) {
    viewModelScope.launch { settings.setMaxAltitudeM(meters) }
  }

  fun setHideGrounded(hide: Boolean) {
    viewModelScope.launch { settings.setHideGrounded(hide) }
  }

  fun setHiddenAircraftCategories(categories: Set<String>) {
    viewModelScope.launch { settings.setHiddenAircraftCategories(categories) }
  }

  fun setHiddenVesselCategories(categories: Set<String>) {
    viewModelScope.launch { settings.setHiddenVesselCategories(categories) }
  }

  fun setHiddenVesselStatuses(statuses: Set<String>) {
    viewModelScope.launch { settings.setHiddenVesselStatuses(statuses) }
  }
}

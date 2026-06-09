package eu.slickbot.avimar.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.slickbot.avimar.data.adsb.AdsbProvider
import eu.slickbot.avimar.data.adsb.AdsbRepository
import eu.slickbot.avimar.data.adsb.Aircraft
import eu.slickbot.avimar.data.adsb.info.AircraftInfo
import eu.slickbot.avimar.data.adsb.info.AircraftInfoProvider
import eu.slickbot.avimar.data.ais.AisConnectionState
import eu.slickbot.avimar.data.ais.AisRepository
import eu.slickbot.avimar.data.ais.Vessel
import eu.slickbot.avimar.data.ais.info.VesselDetails
import eu.slickbot.avimar.data.ais.info.VesselEnricher
import eu.slickbot.avimar.data.ais.translate.AisStreamHealth
import eu.slickbot.avimar.data.location.LatLon
import eu.slickbot.avimar.data.location.LocationProvider
import eu.slickbot.avimar.data.search.RecentEntity
import eu.slickbot.avimar.data.search.SearchHistoryRepository
import eu.slickbot.avimar.data.settings.SettingsRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

sealed interface MapSelection {
  data class Plane(val aircraft: Aircraft) : MapSelection
  data class Ship(val vessel: Vessel) : MapSelection
}

data class CenterCommand(val lat: Double, val lon: Double, val zoom: Double?)

class MapViewModel(
  private val adsbRepo: AdsbRepository,
  private val aisRepo: AisRepository,
  private val settings: SettingsRepository,
  private val locationProvider: LocationProvider,
  private val aircraftInfoProvider: AircraftInfoProvider,
  private val vesselEnricher: VesselEnricher,
  private val searchHistory: SearchHistoryRepository,
) : ViewModel(), SettingsActions {

  companion object {
    private const val TAG = "MapViewModel"
  }

  private val _viewport = MutableStateFlow<Viewport?>(null)
  private val _aircraft = MutableStateFlow<List<Aircraft>>(emptyList())

  // Monotonic mark of the last ADS-B fetch, used to rate-limit across viewport changes.
  private var lastAdsbFetch: TimeSource.Monotonic.ValueTimeMark? = null

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
  }.eager(AircraftFilter.NONE)

  val aircraft: StateFlow<List<Aircraft>> = combine(_aircraft, aircraftFilter) { list, filter ->
    filter.apply(list)
  }.eager(emptyList())

  val markerScale: StateFlow<Float> = settings.markerScale.eager(1.0f)

  val hiddenAircraftCategories: StateFlow<Set<String>> = settings.hiddenAircraftCategories.eager(emptySet())
  val hiddenVesselCategories: StateFlow<Set<String>> = settings.hiddenVesselCategories.eager(emptySet())
  val hiddenVesselStatuses: StateFlow<Set<String>> = settings.hiddenVesselStatuses.eager(emptySet())

  private val vesselFilter: StateFlow<VesselFilter> = combine(
    settings.hiddenVesselCategories,
    settings.hiddenVesselStatuses,
  ) { categories, statuses ->
    VesselFilter(hiddenCategoryCodes = categories, hiddenStatusCodes = statuses)
  }.eager(VesselFilter.NONE)

  val vessels: StateFlow<List<Vessel>> = combine(aisRepo.vessels, vesselFilter) { list, filter ->
    filter.apply(list)
  }.eager(emptyList())

  val aisConnection: StateFlow<AisConnectionState> = aisRepo.connectionState

  val aisHealth: StateFlow<AisStreamHealth?> = flow {
    while (true) {
      emit(aisRepo.health())
      delay(2.seconds)
    }
  }.whileSubscribed(null)

  val provider: StateFlow<AdsbProvider> = settings.adsbProvider.eager(AdsbProvider.AdsbFi)
  val tileStyle: StateFlow<TileStyle> = settings.tileStyle.eager(TileStyle.CartoDark)
  val aisEnabled: StateFlow<Boolean> = settings.aisEnabled.eager(true)
  val trailsEnabled: StateFlow<Boolean> = settings.trailsEnabled.eager(true)
  val aviationVisible: StateFlow<Boolean> = settings.aviationVisible.eager(true)
  val maritimeVisible: StateFlow<Boolean> = settings.maritimeVisible.eager(true)
  val aisApiKey: StateFlow<String?> = settings.aisApiKey.eager(null)

  val settingsState: StateFlow<SettingsState> = combine(
    settings.aisApiKey,
    settings.aisEnabled,
    settings.trailsEnabled,
    settings.aviationVisible,
    settings.maritimeVisible,
    settings.hideGrounded,
    settings.markerScale,
    settings.adsbPollInterval,
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
      adsbPollInterval = values[7] as Duration,
      minAltitudeM = values[8] as Int?,
      maxAltitudeM = values[9] as Int?,
      hiddenAircraftCategories = values[10] as Set<String>,
      hiddenVesselCategories = values[11] as Set<String>,
      hiddenVesselStatuses = values[12] as Set<String>,
    )
  }.eager(SettingsState.Empty)

  val settingsActions: SettingsActions get() = this

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
    .runningFold<Pair<Boolean, MapSelection?>, MapSelection?>(null) { prev, (active, resolved) ->
      when {
        !active -> null
        resolved != null -> resolved
        else -> prev
      }
    }
    .eager(null)

  val recentEntities: StateFlow<List<RecentEntity>> = searchHistory.recentEntities.eager(emptyList())

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
    .whileSubscribed(Loadable.Loading)

  val shipEnrichment: StateFlow<Loadable<VesselDetails>> = _selectedVesselMmsi
    .flatMapLatest { mmsi ->
      if (mmsi == null) {
        flowOf(Loadable.Ready<VesselDetails>(null))
      } else {
        val imo = aisRepo.vessels.value.firstOrNull { it.mmsi == mmsi }?.imo
        loadableFetch { vesselEnricher.lookup(mmsi, imo) }
      }
    }
    .whileSubscribed(Loadable.Loading)

  private val _centerCommands = MutableSharedFlow<CenterCommand>(
    extraBufferCapacity = 4,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )
  val centerCommands: SharedFlow<CenterCommand> = _centerCommands.asSharedFlow()

  init {
    viewModelScope.launch {
      combine(
        _viewport.filterNotNull().debounce(300.milliseconds),
        provider,
        settings.adsbPollInterval,
      ) { vp, p, pollInterval -> Triple(vp, p, pollInterval) }
        .flatMapLatest { (vp, p, pollInterval) ->
          val interval = if (pollInterval > Duration.ZERO) pollInterval else p.minPollInterval
          flow {
            while (true) {
              // Enforce the minimum interval across viewport changes, not just within one
              // poll loop: panning restarts this flow, so without this a new viewport would
              // fetch immediately and ignore the rate limit. flatMapLatest coalesces to the
              // latest viewport while we wait.
              val sinceLast = lastAdsbFetch?.elapsedNow()
              val wait = if (sinceLast == null) Duration.ZERO else interval - sinceLast
              if (wait > Duration.ZERO) delay(wait)
              lastAdsbFetch = TimeSource.Monotonic.markNow()
              fetchAdsb(p, vp)
              emit(Unit)
            }
          }
        }
        .collect {}
    }

    viewModelScope.launch {
      combine(
        aisApiKey,
        aisEnabled,
        _viewport.filterNotNull().debounce(400.milliseconds),
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

  fun selectAircraft(hex: String) {
    _selectedVesselMmsi.value = null
    _selectedAircraftHex.value = hex
    _following.value = true
    _aircraft.value.firstOrNull { it.hex == hex }?.let { recordRecent(RecentEntity.fromAircraft(it)) }
  }

  fun selectVessel(mmsi: Long) {
    _selectedAircraftHex.value = null
    _selectedVesselMmsi.value = mmsi
    _following.value = true
    aisRepo.vessels.value.firstOrNull { it.mmsi == mmsi }?.let { recordRecent(RecentEntity.fromVessel(it)) }
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

  fun onMapGesture() {
    _following.value = false
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

  fun selectProvider(provider: AdsbProvider) { edit { setAdsbProvider(provider) } }
  fun selectTileStyle(style: TileStyle) { edit { setTileStyle(style) } }
  fun setHiddenAircraftCategories(categories: Set<String>) { edit { setHiddenAircraftCategories(categories) } }
  fun setHiddenVesselCategories(categories: Set<String>) { edit { setHiddenVesselCategories(categories) } }
  fun setHiddenVesselStatuses(statuses: Set<String>) { edit { setHiddenVesselStatuses(statuses) } }

  // SettingsActions overrides

  override fun setAisApiKey(value: String?) { edit { setAisApiKey(value) } }
  override fun setAisEnabled(enabled: Boolean) { edit { setAisEnabled(enabled) } }
  override fun setTrailsEnabled(enabled: Boolean) { edit { setTrailsEnabled(enabled) } }
  override fun setAviationVisible(visible: Boolean) { edit { setAviationVisible(visible) } }
  override fun setMaritimeVisible(visible: Boolean) { edit { setMaritimeVisible(visible) } }
  override fun setHideGrounded(hide: Boolean) { edit { setHideGrounded(hide) } }
  override fun setMarkerScale(scale: Float) { edit { setMarkerScale(scale) } }
  override fun setAdsbPollInterval(interval: Duration) { edit { setAdsbPollInterval(interval) } }
  override fun setMinAltitudeM(meters: Int?) { edit { setMinAltitudeM(meters) } }
  override fun setMaxAltitudeM(meters: Int?) { edit { setMaxAltitudeM(meters) } }

  override fun toggleHiddenAircraftCategory(code: String) {
    val current = settingsState.value.hiddenAircraftCategories
    val next = if (code in current) current - code else current + code
    edit { setHiddenAircraftCategories(next) }
  }

  override fun toggleHiddenVesselCategory(code: String) {
    val current = settingsState.value.hiddenVesselCategories
    val next = if (code in current) current - code else current + code
    edit { setHiddenVesselCategories(next) }
  }

  override fun toggleHiddenVesselStatus(code: String) {
    val current = settingsState.value.hiddenVesselStatuses
    val next = if (code in current) current - code else current + code
    edit { setHiddenVesselStatuses(next) }
  }

  // Private helpers

  private fun edit(block: suspend SettingsRepository.() -> Unit) {
    viewModelScope.launch { settings.block() }
  }

  private fun <T> Flow<T>.eager(initial: T): StateFlow<T> {
    return stateIn(viewModelScope, SharingStarted.Eagerly, initial)
  }

  private fun <T> Flow<T>.whileSubscribed(initial: T): StateFlow<T> {
    return stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), initial)
  }

  private fun recordRecent(entity: RecentEntity) {
    viewModelScope.launch { searchHistory.add(entity) }
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
}

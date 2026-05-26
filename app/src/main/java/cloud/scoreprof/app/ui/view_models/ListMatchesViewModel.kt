package cloud.scoreprof.app.ui.view_models

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.scoreprof.app.data.PredictionUpdateRepository
import cloud.scoreprof.app.domain.model.Match
import cloud.scoreprof.app.domain.model.UserPredictionUpdate
import cloud.scoreprof.app.domain.usecase.MatchesUseCases
import cloud.scoreprof.app.ui.components.SortByKickOffAsc
import cloud.scoreprof.app.ui.components.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@HiltViewModel
class ListMatchesViewModel @Inject constructor(
    private val matchesUseCases: MatchesUseCases,
    private val predictionUpdateRepository: PredictionUpdateRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private var refreshJob: Job? = null
    private val _competitionId = MutableStateFlow<String?>(null)
    private val _isNetworkError = MutableStateFlow(false)
    
    private val userid: String = checkNotNull(savedStateHandle.get<String>("userid")) {
        "userid is required for ListMatchesViewModel"
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MatchesUiState> = combine(
        _competitionId.flatMapLatest { id ->
            if (id == null) flowOf(null)
            else matchesUseCases.getMatches(id).onStart { emit(emptyList()) }
        },
        _isNetworkError
    ) { matches, isNetError ->
        when {
            matches == null -> MatchesUiState.Loading
            matches.isEmpty() && isNetError -> MatchesUiState.Error("Unable to load matches. Please check your connection.")
            matches.isEmpty() && refreshJob?.isActive == true -> MatchesUiState.Loading
            matches.isEmpty() -> MatchesUiState.Success(emptyMap(), -1)
            else -> {
                val sortedMatches = matches.sortedBy { it.kickoff }
                val nowInSeconds = OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond()
                val firstUpcomingMatchIndex = sortedMatches.indexOfFirst { match ->
                    (match.kickoff.toEpochSecond() - 60) > nowInSeconds
                }
                val grouped = sortedMatches
                    .groupBy { it.kickoff.toLocalDate() }
                    .toSortedMap()

                MatchesUiState.Success(grouped, firstUpcomingMatchIndex)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MatchesUiState.Loading
    )

    private val _sortOrder = MutableStateFlow<SortOrder>(SortByKickOffAsc)
    var sortOrder = _sortOrder.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            _competitionId.collect { id ->
                id?.let { loadAndCacheMatches(it) }
            }
        }
    }

    fun retryLoading() {
        _competitionId.value?.let { loadAndCacheMatches(it) }
    }

    private fun loadAndCacheMatches(competitionId: String) {
        _isNetworkError.value = false
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                matchesUseCases.loadAndCacheMatches(competitionId, userid)
            } catch (e: Exception) {
                e.printStackTrace()
                _isNetworkError.value = true
                val hasData = matchesUseCases.hasMatches(competitionId)
                if (!hasData) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Connection issue. Showing offline data if available."))
                }
            }
        }
    }

    private fun savePredictionsAndNavigateUp() {
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.NavigateUp)
        }
    }

    private fun applySort(list: List<Match>, order: SortOrder): List<Match> {
        return when (order) {
            SortByKickOffAsc -> list.sortedBy { it.kickoff }
            else -> list
        }
    }

    fun onEvent(event: MatchEvent) {
        when(event) {
            is MatchEvent.OnPredictionMade -> {
                updateLocalPrediction(event.match, event.competitor1selected, event.competitor2selected)
                updateServerPrediction(event.match, event.competitor1selected, event.competitor2selected, UUID.fromString(userid))
            }
            is MatchEvent.Order -> { _sortOrder.value = event.matchOrder }
            is MatchEvent.SaveAndNavigateUp -> { savePredictionsAndNavigateUp() }
            is MatchEvent.LoadMatchesForCompetition -> { _competitionId.value = event.competitionId }
            is MatchEvent.NavigateUp -> { viewModelScope.launch { _eventFlow.emit(UiEvent.NavigateUp) } }
        }
    }

    sealed class UiEvent {
        object NavigateUp : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    sealed class MatchEvent {
        data class Order(val matchOrder: SortOrder) : MatchEvent()
        object SaveAndNavigateUp : MatchEvent()
        object NavigateUp : MatchEvent()
        data class OnPredictionMade(
            val match: Match,
            val competitor1selected: Boolean,
            val competitor2selected: Boolean
        ) : MatchEvent()
        data class LoadMatchesForCompetition(val competitionId: String) : MatchEvent()
    }

    sealed interface MatchesUiState {
        data class Success(
            val groupedMatches: Map<LocalDate, List<Match>>,
            val firstUpcomingMatchIndex: Int = -1
        ) : MatchesUiState
        data object Loading : MatchesUiState
        data class Error(val message: String) : MatchesUiState
    }

    private fun updateLocalPrediction(match: Match, competitor1selected: Boolean, competitor2selected: Boolean) {
        viewModelScope.launch {
            matchesUseCases.updatePredictionInDb(
                matchid = match.id,
                competitor1selected = competitor1selected,
                competitor2selected = competitor2selected
            )
        }
    }

    private fun updateServerPrediction(match: Match, competitor1selected: Boolean, competitor2selected: Boolean, userId: UUID) {
        viewModelScope.launch(Dispatchers.IO) {
            val predictionUpdate = UserPredictionUpdate(
                userid = userId,
                matchid = match.id,
                competitor1selected = competitor1selected,
                competitor2selected = competitor2selected
            )
            predictionUpdateRepository.updatePredictionOnServer(predictionUpdate)
        }
    }
}

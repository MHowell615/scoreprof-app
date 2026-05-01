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
    private val userid: String = checkNotNull(savedStateHandle.get<String>("userid")) {
        "userid is required for ListMatchesViewModel"
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MatchesUiState> = _competitionId.flatMapLatest { id ->
        if (id == null) {
            flowOf(MatchesUiState.Loading) // Show loading if no ID is set
        } else {
            matchesUseCases.getMatches(id)
                .map { matches ->
                    // All the grouping and sorting logic now happens inside this map operator
                    val sortedMatches = matches.sortedBy { it.kickoff }
                    val nowInSeconds = OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond()
                    val firstUpcomingMatchIndex = sortedMatches.indexOfFirst { match ->
                        val deadline = match.kickoff.toEpochSecond() - 60
                        deadline > nowInSeconds
                    }
                    val grouped = sortedMatches
                        .groupBy { it.kickoff.toLocalDate() }
                        .toSortedMap(compareBy { it })

                    // The final result of the map is the Success state
                    MatchesUiState.Success(
                        groupedMatches = grouped,
                        firstUpcomingMatchIndex = firstUpcomingMatchIndex
                    )as MatchesUiState
                }
                .onStart { emit(MatchesUiState.Loading) }
        }
    }.stateIn<MatchesUiState>(
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
                id?.let {
                    loadAndCacheMatches(it)
                }
            }
        }
    }

    // Functionality: Loading from JSON and translating country names.
    // This is now delegated to a use case.
    private fun loadAndCacheMatches(competitionId: String) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                // ALWAYS trigger the network fetch.
                // Because your uiState uses flatMapLatest + Room Flow,
                // the UI will update AUTOMATICALLY the moment this network call
                // saves new data into Room.
                matchesUseCases.loadAndCacheMatches(competitionId, userid.toString())
            } catch (e: Exception) {
                e.printStackTrace()
                // Optional: Only show snackbar if there is literally NO data
                val hasData = matchesUseCases.hasMatches(competitionId)
                if (!hasData) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("No internet connection and no local data found."))
                }
            }
        }
    }

    // Functionality: Save predictions and navigate up.
    private fun savePredictionsAndNavigateUp() {
        viewModelScope.launch {// 1. Check if the API is currently refreshing
            val isRefreshing = refreshJob?.isActive == true

            if (!isRefreshing) {
                val currentState = uiState.value
                if (currentState is MatchesUiState.Success) {
                    // Only perform the bulk save if we are sure we aren't
                    // fighting an active API update.
                    // val matchList = currentState.groupedMatches.values.flatten().map { it.toEntity() }
                    // matchesUseCases.upsertMatches(matchList)
                }
            } else {
                // If refreshing, we trust that the Volley call will write
                // the most up-to-date info to the DB.
                // Our individual 'updateLocalPrediction' calls have already
                // secured the user's input.
            }
            _eventFlow.emit(UiEvent.NavigateUp)
        }
    }

    // This logic is purely for the UI, so it stays in the ViewModel.
    private fun applySort(list: List<Match>, order: SortOrder): List<Match> {
        return when (order) {
            SortByKickOffAsc -> list.sortedBy { it.kickoff }
            else -> list
        }
    }

    fun onEvent(event: MatchEvent) {
        when(event) {
            is MatchEvent.OnPredictionMade -> {
                // The User ID would come from a session manager or shared preferences
                var currentUserId = UUID.fromString("00000000-0000-0000-0000-000000000001")

                // 1. UPDATE THE LOCAL DATABASE (for instant UI feedback)
                updateLocalPrediction(event.match, event.competitor1selected, event.competitor2selected)

                // 2. UPDATE THE SERVER (fire-and-forget in the background)
                updateServerPrediction(event.match, event.competitor1selected, event.competitor2selected, currentUserId)
            }
            is MatchEvent.Order -> {
                _sortOrder.value = event.matchOrder
                val currentState = uiState.value
                if (currentState is MatchesUiState.Success) {
                    val flatList = currentState.groupedMatches.values.flatten()
                    val sortedList = applySort(flatList, event.matchOrder)
                    // We cannot directly update the state here anymore.
                }
                /*val flatList = _groupedMatches.value.values.flatten()
                val sortedList = applySort(flatList, event.matchOrder)
                //_groupedMatches.value = sortedList.groupBy { ZonedDateTimeToLocalDate(it.kickoff) }
                _groupedMatches.value = sortedList.groupBy { it.kickoff.toLocalDate() }*/
            }
            is MatchEvent.SaveAndNavigateUp -> {
                savePredictionsAndNavigateUp()
            }
            is MatchEvent.LoadMatchesForCompetition -> {
                _competitionId.value = event.competitionId
            }
            is MatchEvent.NavigateUp -> {
                viewModelScope.launch { _eventFlow.emit(UiEvent.NavigateUp) }
            }
        }
    }

    // Sealed classes remain unchanged. They are part of the ViewModel's public API.
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

    // Inside or alongside your ListMatchesViewModel class
    sealed interface MatchesUiState {
        data class Success(
            val groupedMatches: Map<LocalDate, List<Match>>,
            val firstUpcomingMatchIndex: Int = -1 // Default to -1 (not found)
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
        viewModelScope.launch(Dispatchers.IO) { // Use IO dispatcher for network calls
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
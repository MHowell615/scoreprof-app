package cloud.scoreprof.app.ui.view_models

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.scoreprof.app.data.ScoreProfDao
import cloud.scoreprof.app.domain.model.LeagueHeader
import cloud.scoreprof.app.domain.model.LeagueTable
import cloud.scoreprof.app.domain.usecase.LeaguesUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ListLeagueViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val leaguesUseCases: LeaguesUseCases,
    private val dao: ScoreProfDao
) : ViewModel() {
    val leagueid: String = savedStateHandle.get<String>("leagueid") ?: ""
    val owneruserid: String = requireNotNull(savedStateHandle.get<String>("owneruserid")) {
        "User ID is required to create a league"
    }

    private val _leagueHeader = MutableStateFlow<LeagueHeader?>(null)
    val leagueHeader = _leagueHeader.asStateFlow()

    private val _leagueTable = MutableStateFlow<List<LeagueTable>>(emptyList())
    var leagueTable = _leagueTable.asStateFlow()

    private val _uiEventFlow = MutableSharedFlow<UiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    private suspend fun loadLeagueTable(
        leagueid: String,
        owneruserid: String,
        sortBy: String = "points",
        jumpToTop: Boolean = false,
        showCurrentSeason: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                leaguesUseCases.getLeagueTable(leagueid, owneruserid, sortBy, jumpToTop, showCurrentSeason)
                    .collect { freshList ->
                        dao.updateLeagueTableCache(leagueid, owneruserid, freshList)
                        _leagueTable.value = freshList
                    }
            } catch (e: Exception) {
                val cachedTable = dao.getLeagueTable(leagueid, owneruserid).firstOrNull() ?: emptyList()
                _leagueTable.value = cachedTable

                if (cachedTable.isEmpty()) {
                    _uiEventFlow.emit(UiEvent.ShowSnackbar("Check connection. No data available."))
                }
            }
        }
    }

    suspend fun onEvent(event: LeagueEvent) {
        when(event) {
            is LeagueEvent.LoadLeagueTable -> {
                loadLeagueTable(
                    event.leagueid,
                    event.owneruserid,
                    event.sortBy,
                    event.jumpToTop,
                    event.showCurrentSeason
                )
            }
        }
    }

    sealed class UiEvent {
        data object NavigateUp : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    sealed class LeagueEvent {
        data class LoadLeagueTable(
            val leagueid: String,
            val owneruserid: String,
            val sortBy: String = "points",
            val jumpToTop: Boolean = false,
            val showCurrentSeason: Boolean = false
        ) : LeagueEvent()
    }
}

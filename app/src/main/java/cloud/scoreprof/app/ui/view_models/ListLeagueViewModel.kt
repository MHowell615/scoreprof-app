package cloud.scoreprof.app.ui.view_models

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import cloud.scoreprof.app.data.ScoreProfDao
import cloud.scoreprof.app.domain.model.LeagueHeader
import cloud.scoreprof.app.domain.model.LeagueTable
import cloud.scoreprof.app.domain.usecase.LeaguesUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ListLeagueViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    application: Application,
    private val leaguesUseCases: LeaguesUseCases,
    private val dao: ScoreProfDao
) : AndroidViewModel(application) {
    val leagueid: String = savedStateHandle.get<String>("leagueid") ?: ""
    val ownerUseridString: String = requireNotNull(savedStateHandle.get<String>("owneruserid")) {
        "User ID is required to create a league"
    }
    val owneruserid: UUID = UUID.fromString(ownerUseridString)

    private val _leagueHeader = MutableStateFlow<LeagueHeader?>(null)
    val leagueHeader = _leagueHeader.asStateFlow()

    private val _leagueTable = MutableStateFlow<List<LeagueTable>>(emptyList())
    var leagueTable = _leagueTable.asStateFlow() // Expose an immutable StateFlow to the UI

    private val _uiEventFlow = MutableSharedFlow<UiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()
    private suspend fun loadLeagueTable(leagueid: String, owneruserid: UUID, sortBy: String = "points") {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Fetch the fresh data from the VPS/PostgREST
                leaguesUseCases.getLeagueTable(leagueid, owneruserid.toString(), sortBy)
                    .collect { freshList ->

                        // 2. Update the local Room database cache
                        dao.updateLeagueTableCache(leagueid, owneruserid, freshList)

                        // 3. Update the StateFlow to refresh the UI
                        _leagueTable.value = freshList
                    }

                // 2. Update the local Room database so the cache is fresh
                // This ensures that next time the user opens the app offline, they see the new points
                //dao.updateLeagueTableCache(leagueid, owneruserid, result)

                // 3. Update the StateFlow to immediately refresh the UI
                //_leagueTable.value = result

                Log.d("ListLeagueViewModel", "Successfully synced league table from VPS.")
            } catch (e: Exception) {
                Log.e("ListLeagueViewModel", "Failed to sync league table", e)

                // Fallback: If network fails, try to load what we have in Room
                val cachedTable = dao.getLeagueTable(leagueid, owneruserid.toString()).firstOrNull() ?: emptyList()
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
                loadLeagueTable(event.leagueid, event.owneruserid, event.sortBy)
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
            val owneruserid: UUID,
            val sortBy: String = "points"
        ) : LeagueEvent()
    }
}
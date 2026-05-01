package cloud.scoreprof.app.ui.view_models

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.scoreprof.app.domain.model.Leagues
import cloud.scoreprof.app.domain.model.Setup
import cloud.scoreprof.app.domain.usecase.LeaguesUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListLeaguesViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val leaguesUseCases: LeaguesUseCases
) : ViewModel() {
    val userid: String = requireNotNull(savedStateHandle.get<String>("userid")) {
        "User ID is required to create a league"
    }
    private val _setup = MutableStateFlow<Setup?>(null)
    val setup = _setup.asStateFlow()

    private val _leagues = MutableStateFlow<List<Leagues>>(emptyList())
    val leagues = _leagues.asStateFlow() // Expose an immutable StateFlow to the UI

    private val _uiEventFlow = MutableSharedFlow<UiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    init {
        loadLeagues()
    }

    fun loadLeagues() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Call the suspend function to get the list directly.
                val leagueList = leaguesUseCases.getLeagues(userid)

                // 2. Transform the list of Leagues into a list of SelectableItem<Leagues>.
                val filteredLeagues = leagueList/*.filter {
                    it.selected && it.state?.uppercase() != "DELETED"
                }*/

                // 3. Update the StateFlow with the new list.
                _leagues.value = filteredLeagues

                Log.d("ListLeaguesViewModel", "Successfully loaded leagues.")
            } catch (e: Exception) {
                Log.e("ListLeaguesViewModel", "Failed to get leagues", e)
                _uiEventFlow.emit(UiEvent.ShowSnackbar("Failed to load leagues"))
            }
        }

    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        // You can add other events like Navigate, ShowDialog, etc.
    }
}
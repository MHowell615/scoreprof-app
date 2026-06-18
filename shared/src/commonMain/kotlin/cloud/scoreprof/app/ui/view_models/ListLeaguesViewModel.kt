package cloud.scoreprof.app.ui.view_models

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.scoreprof.app.domain.model.Leagues
import cloud.scoreprof.app.domain.model.Setup
import cloud.scoreprof.app.domain.usecase.LeaguesUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ListLeaguesViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val leaguesUseCases: LeaguesUseCases
) : ViewModel() {
    val userid: String = requireNotNull(savedStateHandle.get<String>("userid")) {
        "User ID is required to create a league"
    }
    private val _setup = MutableStateFlow<Setup?>(null)
    val setup = _setup.asStateFlow()

    private val _leagues = MutableStateFlow<List<Leagues>>(emptyList())
    val leagues = _leagues.asStateFlow()

    private val _uiEventFlow = MutableSharedFlow<UiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    init {
        loadLeagues()
    }

    fun loadLeagues() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val leagueList = leaguesUseCases.getLeagues(userid)
                _leagues.value = leagueList
            } catch (e: Exception) {
                _uiEventFlow.emit(UiEvent.ShowSnackbar("Failed to load leagues"))
            }
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }
}

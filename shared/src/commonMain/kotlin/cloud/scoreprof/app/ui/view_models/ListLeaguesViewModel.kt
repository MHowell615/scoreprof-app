package cloud.scoreprof.app.ui.view_models

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.scoreprof.app.domain.model.Leagues
import cloud.scoreprof.app.domain.model.Setup
import cloud.scoreprof.app.domain.usecase.LeaguesUseCases
import cloud.scoreprof.app.getStringComparator
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

    private val _groupedLeagues = MutableStateFlow<Map<String, List<Leagues>>>(emptyMap())
    val groupedLeagues = _groupedLeagues.asStateFlow()

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
                
                // Grouping and Sorting
                val publicOwnerId = "00000000-0000-0000-0000-111111111111"
                val comparator = getStringComparator()
                val filteredAndSorted = leagueList
                    .filter { it.state.uppercase() != "DELETED" }
                    .sortedWith { a, b ->
                        // 1. Force "All" to the top
                        val aIsAll = a.leagueid.equals("All", ignoreCase = true)
                        val bIsAll = b.leagueid.equals("All", ignoreCase = true)
                        if (aIsAll && !bIsAll) return@sortedWith -1
                        if (!aIsAll && bIsAll) return@sortedWith 1
                        
                        // 2. Otherwise sort by name using locale-aware comparator
                        comparator.compare(a.name, b.name)
                    }

                val grouped = LinkedHashMap<String, List<Leagues>>()
                
                val privateLeagues = filteredAndSorted.filter { 
                    it.owneruserid != publicOwnerId && !it.leagueid.equals("All", ignoreCase = true) 
                }
                if (privateLeagues.isNotEmpty()) grouped["Private"] = privateLeagues

                val publicLeagues = filteredAndSorted.filter { 
                    it.owneruserid == publicOwnerId || it.leagueid.equals("All", ignoreCase = true) 
                }
                if (publicLeagues.isNotEmpty()) grouped["Public"] = publicLeagues

                _groupedLeagues.value = grouped

            } catch (e: Exception) {
                _uiEventFlow.emit(UiEvent.ShowSnackbar("Failed to load leagues"))
            }
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }
}

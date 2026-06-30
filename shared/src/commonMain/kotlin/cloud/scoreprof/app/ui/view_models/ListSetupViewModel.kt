package cloud.scoreprof.app.ui.view_models

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.scoreprof.app.data.LeaguesRepository
import cloud.scoreprof.app.data.ScoreProfDao
import cloud.scoreprof.app.data.SetupRepository
import cloud.scoreprof.app.data.BillingManager
import cloud.scoreprof.app.domain.model.Competition
import cloud.scoreprof.app.domain.model.Language
import cloud.scoreprof.app.domain.model.LeagueHeader
import cloud.scoreprof.app.domain.model.Leagues
import cloud.scoreprof.app.domain.model.Setup
import cloud.scoreprof.app.domain.model.UserCompetitionSelection
import cloud.scoreprof.app.domain.model.UserLeague
import cloud.scoreprof.app.domain.usecase.LanguagesUseCases
import cloud.scoreprof.app.domain.usecase.SetupUseCases
import cloud.scoreprof.app.data.local.TokenManager
import cloud.scoreprof.app.Platform
import cloud.scoreprof.app.getStringComparator
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class SelectableItem<T>(val item: T, var isSelected: Boolean)

class ListSetupViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val setupUseCases: SetupUseCases,
    private val setupRepository: SetupRepository,
    private val leaguesRepository: LeaguesRepository,
    private val languagesUseCases: LanguagesUseCases,
    private val tokenManager: TokenManager,
    private val billingManager: BillingManager,
    private val dao: ScoreProfDao,
    private val httpClient: HttpClient,
    private val platform: Platform
) : ViewModel() {

    val userid: String = try {
        val id = savedStateHandle.get<String>("userid")
        if (!id.isNullOrBlank()) {
            id
        } else {
            tokenManager.getUserId() ?: throw IllegalArgumentException("User ID missing")
        }
    } catch (e: Exception) {
        println("ScoreProf Fatal Init Error: ${e.message}")
        "00000000-0000-0000-0000-000000000000"
    }

    private val _setup = MutableStateFlow<Setup?>(null)
    val setup = _setup.asStateFlow()

    private val _competitions = MutableStateFlow<List<SelectableItem<Competition>>>(emptyList())
    val competitions = _competitions.asStateFlow()
    private val _setupLeagues = MutableStateFlow<List<SelectableItem<Leagues>>>(emptyList())
    val setupLeagues = _setupLeagues.asStateFlow()

    private val _languages = MutableStateFlow<List<SelectableItem<Language>>>(emptyList())
    val languages = _languages.asStateFlow()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    private val _showOnlyUpcoming = MutableStateFlow(false)
    val showOnlyUpcoming = _showOnlyUpcoming.asStateFlow()

    init {
        loadInitialDataForUser(userid)

        viewModelScope.launch {
            billingManager.purchaseSuccess.collect { success ->
                if (success) {
                    onAdsRemovedSuccessfully()
                }
            }
        }
    }

    fun toggleUpcomingFilter(enabled: Boolean) {
        _showOnlyUpcoming.value = enabled
    }

    private fun onAdsRemovedSuccessfully() {
        val currentSetup = _setup.value ?: return
        val updatedSetup = currentSetup.copy(is_ads_removed = true)
        _setup.value = updatedSetup

        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.insertSetup(updatedSetup)
                setupRepository.updateAdsRemoved(true)
            } catch (e: Exception) {
                println("Failed to sync ad removal status: ${e.message}")
            }
        }
    }

    fun loadInitialDataForUser(userid: String) {
        if (userid == "guest") {
            _setup.value = Setup(
                id = 0,
                version = 1,
                email = "guest@scoreprof.cloud",
                name = "Guest User",
                userid = "guest",
                memberSince = null,
                preferred_language = platform.language,
                competitions = emptyList(),
                leagues = emptyList(),
                receive_email = false,
                is_ads_removed = false
            )
            return
        }

        viewModelScope.launch {
            setupRepository.getSetup(userid).collect { setupFromDb ->
                if (setupFromDb != null) {
                    _setup.value = setupFromDb
                    loadSelectionListsFromDb(setupFromDb)
                }
            }
        }

        viewModelScope.launch {
            try {
                if (userid == "guest") return@launch

                // Ensure server is updated with current platform language before refreshing
                val currentLang = platform.language
                println("HomeScreen loaded. Ensuring server lang sync: $currentLang")
                setupRepository.updateLanguage(currentLang)
                
                // Now refresh from server using the current platform language
                setupRepository.refreshSetupFromServer(userid, currentLang)
            } catch (e: Exception) {
                if (e.message == "SESSION_EXPIRED") {
                    _navigationEvents.emit(NavigationEvent.ToLogin)
                }
            }
        }
    }

    fun refreshData() {
        val userId = _setup.value?.userid ?: return
        
        viewModelScope.launch {
            try {
                // Use platform.language here as it's the most up-to-date choice
                setupRepository.refreshSetupFromServer(userId, platform.language)
            } catch (e: Exception) {
                println("Refresh failed: ${e.message}")
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                setupRepository.logout()
                _setup.value = null
                onSuccess()
            } catch (e: Exception) {
                onSuccess()
            }
        }
    }

    fun softDeleteLeague(league: Leagues) {
        viewModelScope.launch {
            try {
                val deletedLeague = league.copy(state = "Deleted", selected = false)
                dao.softDeleteLeague(
                    leagueid = deletedLeague.leagueid,
                    owneruserid = deletedLeague.owneruserid,
                    state = deletedLeague.state,
                    isSelected = deletedLeague.selected
                )

                leaguesRepository.softDeleteLeague(leagueid = league.leagueid, owneruserid = league.owneruserid)

                _setupLeagues.update { currentList ->
                    currentList.map {
                        if (it.item.leagueid == league.leagueid)
                            it.copy(item = deletedLeague, isSelected = false)
                        else it
                    }
                }

                _setup.update { currentSetup ->
                    currentSetup?.copy(
                        leagues = currentSetup.leagues.map {
                            if (it.leagueid == league.leagueid)
                                it.copy(state = "Deleted", selected = false)
                            else it
                        }
                    )
                }
            } catch (e: Exception) {
                println("Error soft-deleting league: ${e.message}")
            }
        }
    }

    fun changePassword(newPassword: String) {
        val userId = tokenManager.getUserId() ?: return
        if (newPassword.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val body = buildJsonObject {
                    put("u_id", userId)
                    put("new_password", newPassword.trim())
                }

                val url = "https://www.scoreprof.cloud/rpc/change_password"
                val responseString: String = httpClient.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }.body()

                val result = Json.parseToJsonElement(responseString)
                val newToken = (result as? kotlinx.serialization.json.JsonObject)?.get("new_token")?.toString()?.trim('"')
                if (newToken != null) {
                    tokenManager.saveToken(newToken)
                    _uiState.value = HomeUiState.Error("Password updated successfully.")
                } else {
                    _uiState.value = HomeUiState.Error("Update failed.")
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error("Connection error.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadSelectionListsFromDb(setupData: Setup) {
        _competitions.value = setupData.competitions.map { userCompetition ->
            SelectableItem(
                item = Competition(
                    id = userCompetition.id,
                    competitionid = userCompetition.competitionid,
                    sport_type = userCompetition.sport_type,
                    region = userCompetition.region,
                    country_ranking = userCompetition.country_ranking,
                    name = userCompetition.name,
                    has_upcoming = userCompetition.has_upcoming
                ),
                isSelected = userCompetition.selected ?: false
            )
        }

        _setupLeagues.value = setupData.leagues.map { userLeague ->
            SelectableItem(
                item = Leagues(
                    id = userLeague.id,
                    leagueid = userLeague.leagueid,
                    competitionid = userLeague.competitionid,
                    owneruserid = userLeague.owneruserid,
                    leaguecode = userLeague.leaguecode,
                    name = userLeague.name,
                    state = userLeague.state ?: "Active",
                    invited = userLeague.invited ?: false,
                    selected = userLeague.selected ?: false
                ),
                isSelected = userLeague.selected ?: false
            )
        }
    }

    fun loadLanguages() {
        viewModelScope.launch(Dispatchers.IO) {
            val preferredLanguage = _setup.value?.preferred_language ?: "en"

            try {
                val allLanguages = languagesUseCases.getLanguages(preferredLanguage)
                _languages.value = allLanguages.map { language ->
                    SelectableItem(
                        item = language,
                        isSelected = language.languageCode == preferredLanguage
                    )
                }
            } catch (e: Exception) {
                println("Failed to get languages: ${e.message}")
            }
        }
    }

    fun onToggleAllCompetitions(isSelected: Boolean) {
        viewModelScope.launch {
            try {
                val currentList = _competitions.value
                val updatedList = currentList.map { it.copy(isSelected = isSelected) }
                _competitions.value = updatedList

                setupUseCases.updateAllUserCompetitions(isSelected)

                val selections = updatedList.map { selectable ->
                    UserCompetitionSelection(
                        id = selectable.item.id,
                        competitionid = selectable.item.competitionid,
                        sport_type = selectable.item.sport_type,
                        name = selectable.item.name,
                        selected = isSelected
                    )
                }
                dao.updateAllCompetitions(selections)

                _setup.update { currentSetup ->
                    currentSetup?.copy(
                        competitions = currentSetup.competitions.map { it.copy(selected = isSelected) }
                    )
                }

            } catch (e: Exception) {
                println("Failed to batch update competitions: ${e.message}")
            }
        }
    }

    fun onCompetitionSelected(competition: Competition, isSelected: Boolean) {
        viewModelScope.launch {
            try {
                setupUseCases.updateUserCompetition(competition.competitionid, isSelected)
                val userCompetitionSelection = UserCompetitionSelection(
                    id = competition.id,
                    competitionid = competition.competitionid,
                    sport_type = competition.sport_type,
                    name = competition.name,
                    selected = isSelected
                )
                dao.updateUserCompetition(userCompetitionSelection)

                _competitions.update { currentList ->
                    currentList.map { selectableItem ->
                        if (selectableItem.item.competitionid == competition.competitionid) {
                            selectableItem.copy(isSelected = isSelected)
                        } else {
                            selectableItem
                        }
                    }
                }

                _setup.update { currentSetup ->
                    currentSetup?.copy(
                        competitions = currentSetup.competitions.map { item ->
                            if (item.competitionid == competition.competitionid) {
                                item.copy(selected = isSelected)
                            } else {
                                item
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                println("Failed to update competition: ${e.message}")
            }
        }
    }

    fun sendFeedback(category: String, subject: String, description: String) {
        viewModelScope.launch {
            setupUseCases.sendSupportEmail(category, subject, description)
        }
    }

    fun onLeagueSelected(league: Leagues, isSelected: Boolean) {
        _setupLeagues.update { currentList ->
            currentList.map { selectableItem ->
                if (selectableItem.item.leagueid == league.leagueid) {
                    selectableItem.copy(isSelected = isSelected)
                } else {
                    selectableItem
                }
            }
        }

        _setup.update { currentSetup ->
            currentSetup?.copy(
                leagues = currentSetup.leagues.map {
                    if (it.leagueid == league.leagueid) {
                        it.copy(selected = isSelected)
                    } else {
                        it
                    }
                }
            )
        }
        viewModelScope.launch {
            setupRepository.updateUserLeague(league.leagueid, league.owneruserid, isSelected)
        }
    }

    fun saveSetupScreenChanges() {
        val currentSetup = _setup.value ?: return
        viewModelScope.launch {
            val selectedLanguage = currentSetup.preferred_language
            setupRepository.upsertSetup(currentSetup)
            setupRepository.updateUserProfile(
                name = currentSetup.name?.ifBlank { currentSetup.email.substringBefore('@') } ?: "",
                email = currentSetup.email,
                language = selectedLanguage
            )
        }
    }

    fun requestJoinLeague(joinCode: String) {
        viewModelScope.launch {
            setupRepository.requestJoinLeague(joinCode)
        }
    }

    fun onSetupDetailChanged(email: String, name: String) {
        val currentSetup = _setup.value ?: return
        val updatedSetup = currentSetup.copy(email = email, name = name)
        _setup.value = updatedSetup
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertSetup(updatedSetup)
        }
    }

    fun onLanguageChanged(newLanguage: Language) {
        val currentSetup = _setup.value ?: return

        val updatedSetup = currentSetup.copy(preferred_language = newLanguage.languageCode)
        _setup.value = updatedSetup

        _languages.update { currentList ->
            currentList.map {
                it.copy(isSelected = it.item.languageCode == newLanguage.languageCode)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            dao.insertSetup(updatedSetup)
            // Notify server immediately of the language change
            setupRepository.updateLanguage(newLanguage.languageCode)
            // Refresh setup to get localized competition names etc.
            setupRepository.refreshSetupFromServer(userid, newLanguage.languageCode)
        }

        // Apply to the system/platform (Android 13+ support)
        platform.setLanguage(newLanguage.languageCode)
    }

    fun onPrivacySettingsChanged(receiveEmail: Boolean, receiveNotifications: Boolean) {
        val currentSetup = _setup.value ?: return
        val updatedSetup = currentSetup.copy(
            receive_email = receiveEmail,
            receive_notifications = receiveNotifications
        )
        _setup.value = updatedSetup
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.insertSetup(updatedSetup)
                setupRepository.updateUserPrivacy(receiveEmail, receiveNotifications)
            } catch (e: Exception) {
                println("Privacy update failed: ${e.message}")
            }
        }
    }

    sealed class HomeUiState {
        data object Idle : HomeUiState()
        data object Loading : HomeUiState()
        data class Error(val message: String) : HomeUiState()
    }

    sealed class NavigationEvent {
        data object ToLogin : NavigationEvent()
    }
}

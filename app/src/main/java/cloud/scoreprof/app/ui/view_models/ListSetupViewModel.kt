package cloud.scoreprof.app.ui.view_models

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.volley.toolbox.Volley
import cloud.scoreprof.app.data.LeaguesRepository
import cloud.scoreprof.app.data.ScoreProfDao
import cloud.scoreprof.app.data.SetupRepository
import cloud.scoreprof.app.data.BillingManager
import cloud.scoreprof.app.domain.model.Competition
import cloud.scoreprof.app.domain.model.Language
import cloud.scoreprof.app.domain.model.LeagueHeader
import cloud.scoreprof.app.domain.model.LeagueState
import cloud.scoreprof.app.domain.model.Leagues
import cloud.scoreprof.app.domain.model.Setup
import cloud.scoreprof.app.domain.model.SetupPayload
import cloud.scoreprof.app.domain.model.UpdateLeagueRequest
import cloud.scoreprof.app.domain.model.UserCompetitionSelection
import cloud.scoreprof.app.domain.model.UserCompetitionState
import cloud.scoreprof.app.domain.model.UserLeague
import cloud.scoreprof.app.domain.model.UserLeagueSelection
import cloud.scoreprof.app.domain.model.UserLeagueState
import cloud.scoreprof.app.domain.usecase.LanguagesUseCases
import cloud.scoreprof.app.domain.usecase.SetupUseCases
import cloud.scoreprof.app.ui.utils.SelectableItem
import cloud.scoreprof.app.data.local.TokenManager
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

data class SelectableItem<T>(val item: T, var isSelected: Boolean)

@HiltViewModel
class ListSetupViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val application: Application,
    private val setupUseCases: SetupUseCases,
    private val setupRepository: SetupRepository,
    private val leaguesRepository: LeaguesRepository,
    private val languagesUseCases: LanguagesUseCases,
    private val tokenManager: TokenManager,
    private val billingManager: BillingManager,
    private val dao: ScoreProfDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val userid: UUID = try {
        val uuidString = savedStateHandle.get<String>("userid")
        if (!uuidString.isNullOrBlank()) {
            UUID.fromString(uuidString)
        } else {
            val fallbackId = tokenManager.getUserId()
            if (fallbackId != null) {
                UUID.fromString(fallbackId)
            } else {
                // This triggers the 'catch' block below
                throw IllegalArgumentException("User ID missing from Nav and Token.")
            }
        }
    } catch (e: Exception) {
        // 1. Log locally for Logcat
        Log.e("ScoreProf", "Fatal Init Error: ${e.message}")

        // 2. REMOTE LOGGING: Send to your Droplet even though we don't have a userid
        // We use viewModelScope.launch to not block the main thread
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Assuming you add this function to setupRepository
                setupRepository.reportCrash(
                    errorMessage = e.message ?: "Null UserID on Startup",
                    stackTrace = Log.getStackTraceString(e),
                    appVersion = "1.0.7"
                )
            } catch (remoteErr: Exception) {
                // If the server is also down, we just fail silently
            }
        }

        // 3. FAIL-SAFE: Return a temporary ID so the app doesn't crash
        // This keeps the 14-day test running!
        UUID.randomUUID()
    }

    private val requestQueue = Volley.newRequestQueue(context)

    private val _setup = MutableStateFlow<Setup?>(null)
    val setup = _setup.asStateFlow()

    private var _originalSetup: Setup? = null

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

    init {
        loadInitialDataForUser(userid)
        
        // Listen for successful billing events
        viewModelScope.launch {
            billingManager.purchaseSuccess.collect { success ->
                if (success) {
                    onAdsRemovedSuccessfully()
                }
            }
        }
    }

    private fun onAdsRemovedSuccessfully() {
        val currentSetup = _setup.value ?: return
        val updatedSetup = currentSetup.copy(is_ads_removed = true)
        _setup.value = updatedSetup

        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.insertSetup(updatedSetup)
                setupRepository.updateAdsRemoved(true)
                Log.d("ListSetupViewModel", "Ads removed successfully and synced to server.")
            } catch (e: Exception) {
                Log.e("ListSetupViewModel", "Failed to sync ad removal status", e)
            }
        }
    }

    fun triggerRemoveAdsPurchase(activity: Activity) {
        billingManager.launchPurchaseFlow(activity, "remove_ads_premium")
    }

    fun loadInitialDataForUser(userid: UUID) {
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
                setupRepository.refreshSetupFromServer(userid)
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
                setupRepository.refreshSetupFromServer(userId)
            } catch (e: Exception) {
                Log.e("ListSetupViewModel", "Refresh failed", e)
            }
        }
    }

    fun activateUserAccount(email: String, preferredLanguage: String) {
        viewModelScope.launch {
            setupRepository.activateAccount(email, preferredLanguage)
            refreshData()
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
                println("TEST: softDeleteLeague() called for ${league.leagueid}")
                // 1. Update the database state to 'Deleted'
                val deletedLeague = league.copy(state = "Deleted", selected = false)
                dao.softDeleteLeague(
                    leagueid = deletedLeague.leagueid,
                    owneruserid = deletedLeague.owneruserid,
                    state = deletedLeague.state,
                    deletedLeague.selected
                )

                val request = UpdateLeagueRequest(
                    original_leagueid = league.leagueid,
                    original_owneruserid = league.owneruserid,
                    new_league_header = LeagueHeader(
                        leagueid = league.leagueid,
                        owneruserid = league.owneruserid,
                        competitionid = league.competitionid,
                        leagueusers = emptyList() // The SQL handles the state change
                    ),
                    // This wrapper matches the "userleagueusers" key expected by Postgres
                    new_user_league = UserLeague(userleagueusers = emptyList())
                )

                leaguesRepository.softDeleteLeague(leagueid = league.leagueid, owneruserid = league.owneruserid)

                // 2. Update UI list: Don't remove it, just update its internal state
                _setupLeagues.update { currentList ->
                    currentList.map {
                        if (it.item.leagueid == league.leagueid)
                            it.copy(item = deletedLeague, isSelected = false)
                        else it
                    }
                }

                // 3. Update the main Setup object: Again, update don't filter
                _setup.update { currentSetup ->
                    currentSetup?.copy(
                        leagues = currentSetup.leagues.map {
                            if (it.leagueid == league.leagueid)
                                it.copy(state = "Deleted", selected = false)
                            else it
                        }
                    )
                }

                Log.d("SoftDelete", "League ${league.name} state set to Deleted")
            } catch (e: Exception) {
                Log.e("ViewModel", "Error soft-deleting league", e)
            }
        }
    }

    fun changePassword(newPassword: String) {
        val userId = tokenManager.getUserId() ?: return
        if (newPassword.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            val params = JSONObject().apply {
                put("u_id", userId)
                put("new_password", newPassword.trim())
            }

            val url = "https://www.scoreprof.cloud/rpc/change_password"
            val request = object : StringRequest(
                Method.POST, url,
                { response ->
                    try {
                        val result = JSONObject(response)
                        val newToken = result.getString("new_token")
                        tokenManager.saveToken(newToken)
                        _isLoading.value = false
                        _uiState.value = HomeUiState.Error("Password updated successfully.")
                    } catch (e: Exception) {
                        _isLoading.value = false
                        _uiState.value = HomeUiState.Error("Update failed.")
                    }
                },
                { error ->
                    _isLoading.value = false
                    _uiState.value = HomeUiState.Error("Connection error.")
                }
            ) {
                override fun getBody(): ByteArray = params.toString().toByteArray(Charsets.UTF_8)
                override fun getBodyContentType(): String = "application/json; charset=utf-8"
            }
            request.retryPolicy = DefaultRetryPolicy(20000, 0, 1f)
            requestQueue.add(request)
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
                    name = userCompetition.name
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
                // Use the UseCase to get all available languages.
                // This could be from a local file, network, or cached in DB.
                val allLanguages = languagesUseCases.getLanguages(preferredLanguage)
                Log.d("ListSetupViewModel", "Successfully loaded languages.")

                // Create the UI list with the correct item selected
                _languages.value = allLanguages.map { language ->
                    SelectableItem(
                        item = language,
                        isSelected = language.languageCode == preferredLanguage
                    )
                }
            } catch (e: Exception) {
                Log.e("ListSetupViewModel", "Failed to get languages", e)
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
                Log.e("ListSetupViewModel", "Failed to update competition", e)
            }
        }
    }

    fun sendFeedback(category: String, subject: String, description: String) {
        val userEmail = _setup.value?.email ?: "Unknown"
        viewModelScope.launch {
            setupRepository.sendSupportEmail(userEmail, category, subject, description)
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
            val deviceLanguage = java.util.Locale.getDefault().language
            setupRepository.upsertSetup(currentSetup)
            setupRepository.updateUserProfile(
                name = currentSetup.name!!.ifBlank { currentSetup.email.substringBefore('@') },
                email = currentSetup.email,
                language = deviceLanguage
            )
        }
    }

    fun requestJoinLeague(joinCode: String) {
        viewModelScope.launch {
            setupRepository.requestJoinLeague(joinCode)
        }
    }

    /*fun saveChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSetup = _setup.value
            val originalSetup = _originalSetup

            if (currentSetup == originalSetup) {
                Log.i("ViewModel_Save", "No changes detected. Skipping save operation.")
                // You could optionally emit an event here to show a "No changes to save" toast.
                // _eventFlow.emit(ListMatchesViewModel.UiEvent.ShowSnackbar("No changes to save."))
                return@launch
            }

            if (currentSetup == null) {
                Log.e("ViewModel_Save", "Cannot save, setup data is null.")
                return@launch
            }

            // --- Step 1: Update the local database (the DAO pattern you mentioned) ---

            // Create updated lists of selections based on the current UI state
            val selectedCompetitionsToSaveLocal = _competitions.value.map { selectableItem ->
                UserCompetitionSelection(
                    id = selectableItem.item.id,
                    competitionid = selectableItem.item.competitionid,
                    sport_type = selectableItem.item.sport_type,
                    name = selectableItem.item.name,
                    selected = selectableItem.isSelected
                )
            }

            val selectedCompetitionsToSaveServer: List<UserCompetitionState> =
                _competitions.value.map { selectableItem ->
                    UserCompetitionState(
                        competitionid = selectableItem.item.competitionid,
                        selected = selectableItem.isSelected
                    )
                }


            val selectedLeaguesToSaveLocal = _setupLeagues.value.map { selectableItem ->
                UserLeagueSelection(
                    id = selectableItem.item.id,
                    leagueid = selectableItem.item.leagueid,
                    competitionid = selectableItem.item.competitionid,
                    owneruserid = selectableItem.item.owneruserid,
                    leaguecode = selectableItem.item.leaguecode,
                    name = selectableItem.item.name,
                    state = selectableItem.item.state,
                    selected = selectableItem.isSelected,
                    invited = selectableItem.item.invited
                )
            }

            val selectedUserLeaguesToSaveServer: List<UserLeagueState> =
                _setupLeagues.value.map { selectableItem ->
                    UserLeagueState(
                        userid = currentSetup.userid,
                        leagueid = selectableItem.item.leagueid,
                        owneruserid = selectableItem.item.owneruserid,
                        selected = selectableItem.isSelected,
                        invited = selectableItem.item.invited // Pass the existing 'invited' status
                    )
                }

            val selectedLeaguesToSaveServer: List<LeagueState> =
                _setupLeagues.value.map { selectableItem ->
                    LeagueState(
                        leagueid = selectableItem.item.leagueid,
                        owneruserid = selectableItem.item.owneruserid,
                        state = selectableItem.item.state,
                        leaguename = selectableItem.item.name
                    )
                }

            // Create the fully updated Setup object
            val updatedSetupForDb = currentSetup.copy(
                competitions = selectedCompetitionsToSaveLocal,
                leagues = selectedLeaguesToSaveLocal
            )

            val payload = SetupPayload(
                userid = currentSetup.userid,
                name = currentSetup.name,
                memberSince = currentSetup.memberSince.toString(),
                preferredLanguage = currentSetup.preferred_language,
                competitions = selectedCompetitionsToSaveServer,
                userleagues = selectedUserLeaguesToSaveServer,
                leagues = selectedLeaguesToSaveServer
            )
            println("TEST: $payload")

            try {
                _originalSetup = currentSetup
                Log.d("ViewModel_Save", "Sending payload to DAO...")
                // Save the complete object to Room. The collector will automatically update the UI.
                dao.insertSetup(updatedSetupForDb)
                _setup.value = updatedSetupForDb
                Log.d("ViewModel_Save", "Saved selections locally to Room.")

                // --- Step 2: Send the update to the server ---
                // Build the payload for the server using only the IDs
                Log.d("ViewModel_Save", "Sending payload to server: $payload")
                //setupRepository.updateAllSetupOnServer(payload)
                _eventFlow.emit(ListMatchesViewModel.UiEvent.ShowSnackbar("Settings Saved!"))
            } catch (e: Exception) {
                Log.e("ViewModel_Save", "Failed to save setup.", e)
                _eventFlow.emit(ListMatchesViewModel.UiEvent.ShowSnackbar("Error: Could not save settings to server."))
            }
        }
    }*/

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

        // --- STEP 1: UPDATE THE UI STATE IMMEDIATELY ---
        val updatedSetup = currentSetup.copy(preferred_language = newLanguage.languageCode)
        _setup.value = updatedSetup

        // Also update the selectable list for immediate visual feedback
        _languages.update { currentList ->
            currentList.map {
                it.copy(isSelected = it.item.languageCode == newLanguage.languageCode)
            }
        }
        Log.d("ViewModel_Update", "UI state updated immediately with language: ${newLanguage.languageCode}")

        // --- STEP 2: SAVE TO THE DATABASE IN THE BACKGROUND ---
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertSetup(updatedSetup)
            Log.d("ViewModel_Update", "Language change saved to database in the background.")
        }
    }

    fun onPrivacySettingsChanged(receiveEmail: Boolean) {
        val currentSetup = _setup.value ?: return
        val updatedSetup = currentSetup.copy(receive_email = receiveEmail)
        _setup.value = updatedSetup
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.insertSetup(updatedSetup)
                setupRepository.updateUserPrivacy(receiveEmail)
            } catch (e: Exception) {
                Log.e("ListSetupViewModel", "Privacy update failed", e)
            }
        }
    }

    sealed class HomeUiState {
        object Idle : HomeUiState()
        object Loading : HomeUiState()
        data class Error(val message: String) : HomeUiState()
    }

    sealed class NavigationEvent {
        object ToLogin : NavigationEvent()
    }
}

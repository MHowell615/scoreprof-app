package cloud.scoreprof.app.ui.view_models

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
    private val dao: ScoreProfDao,
    @ApplicationContext private val context: Context
) : ViewModel() {
    /*private val userid: UUID? = savedStateHandle.get<String>("userid")?.let {
        try { UUID.fromString(it) } catch (e: Exception) { null }
    }*/
    val userid: UUID = try {
        // Log all available keys to the Logcat so we can see why it's failing
        val keys = savedStateHandle.keys()
        Log.d("ListSetupViewModel", "Available keys: $keys")

        val uuidString = savedStateHandle.get<String>("userid")
        Log.d("ListSetupViewModel", "Value for 'userid': $uuidString")

        if (uuidString != null) {
            UUID.fromString(uuidString)
        } else {
            // Provide a clearer error message including the keys found
            throw IllegalArgumentException("User ID missing. Available keys: $keys")
        }
    } catch (e: Exception) {
        if (e is IllegalArgumentException) throw e
        Log.e("ListSetupViewModel", "UUID Parsing failed", e)
        // Fallback or throw
        throw IllegalArgumentException("User ID is required. Invalid or missing: ${savedStateHandle.get<String>("userid")}")
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
    private val _eventFlow = MutableSharedFlow<ListMatchesViewModel.UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState = _uiState.asStateFlow()

    //private val _isLoading = mutableStateOf(false)
    //val isLoading: State<Boolean> = _isLoading
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    init {
        loadInitialDataForUser(userid)
    }

    fun loadInitialDataForUser(userid: UUID) {
        // 1. Start observing the local database immediately.
        //    This will show stale data instantly, then update automatically.
        viewModelScope.launch {
            // 1. Observe the database
            setupRepository.getSetup(userid).collect { setupFromDb ->
                if (setupFromDb != null) {
                    // Before we pass the data to the UI, we filter the leagues
                    // nested inside the setup object to remove "Deleted" ones.
                    val filteredLeagues = setupFromDb.leagues/*.filter {
                        val normalizedState = it.state?.uppercase() ?: "ACTIVE"
                        normalizedState != "DELETED"
                    }*/
                    val filteredSetup = setupFromDb.copy(leagues = filteredLeagues)

                    // Now the 'setup' StateFlow only contains active leagues
                    _setup.value = filteredSetup

                    // Update the selectable lists (Checkboxes) for the UI
                    loadSelectionListsFromDb(filteredSetup)
                }
            }
        }

        // 2. Parallel server refresh (keep as is)
        viewModelScope.launch {
            try {
                setupRepository.refreshSetupFromServer(userid)
            } catch (e: Exception) {
                if (e.message == "SESSION_EXPIRED") {
                    // IMPORTANT: This state must be observed by your NavHost
                    // to trigger: navController.navigate("login") { popUpTo(0) }
                    _uiState.value = HomeUiState.Error("Session Expired. Please log in again.")

                    // If you have a side-effect flow for navigation:
                    _navigationEvents.emit(NavigationEvent.ToLogin)
                }

                Log.e("ViewModel", "Refresh failed", e)
            }
        }
    }

    fun refreshData() {
        val userId = _setup.value?.userid ?: return
        viewModelScope.launch {
            try {
                // Just refresh from the server. The Flow will handle the UI update.
                setupRepository.refreshSetupFromServer(userId)
            } catch (e: Exception) {
                // Handle error
            }
        }
        Log.i("ListSetupViewModel", "Data refresh triggered.")
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
                Log.e("ListSetupViewModel", "Logout failed", e)
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
            val params = JSONObject().apply {put("u_id", userId)
                put("new_password", newPassword.trim())
            }

            val url = "https://www.scoreprof.cloud/rpc/change_password"
            println("TEST: $url")
            val request = object : StringRequest(
                Method.POST, url,
                { response ->
                    try {
                        // 1. Parse the new token from the server response
                        val result = JSONObject(response)
                        val newToken = result.getString("new_token")

                        // 2. SAVE THE NEW TOKEN IMMEDIATELY
                        tokenManager.saveToken(newToken)
                        _isLoading.value = false
                        println("ScoreProfLog: Password changed and token refreshed.")
                        _uiState.value = HomeUiState.Error("Password updated successfully.")
                    } catch (e: Exception) {
                        _isLoading.value = false
                        _uiState.value = HomeUiState.Error("Password changed, please log in again.")
                        println("Change password failed: ${e.message}")
                    }
                },
                { error ->
                    _isLoading.value = false
                    val serverData = error.networkResponse?.data?.let { String(it) }
                    viewModelScope.launch {
                        _uiState.value = HomeUiState.Error("Update failed: $serverData")
                    }
                }
            ) {
                override fun getBody(): ByteArray = params.toString().toByteArray(Charsets.UTF_8)
                override fun getBodyContentType(): String = "application/json; charset=utf-8"
            }

            request.retryPolicy =
                DefaultRetryPolicy(20000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            requestQueue.add(request)
        }
    }

    private fun loadSelectionListsFromDb(setupData: Setup) {
        // This assumes your `getSetupData` API call returns the *master list* of all
        // available competitions/leagues, with a `selected` flag for each.
        _competitions.value = setupData.competitions.map { userCompetition ->
            SelectableItem(
                // We need to create a full Competition object for the UI
                item = Competition(
                    id = userCompetition.id,
                    competitionid = userCompetition.competitionid,
                    sport_type = userCompetition.sport_type,
                    region = userCompetition.region,
                    country_ranking = userCompetition.country_ranking,
                    name = userCompetition.name,
                    //localizedNames = Competition.LocalizedNames(en = userCompetition.name) // Placeholder
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
                // update server
                setupUseCases.updateUserCompetition(competition.competitionid, isSelected)

                // update locally
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
                //observeAndRefreshSetupData(userid)
            } catch (e: Exception) {
                Log.e("ListSetupViewModel", "Failed to update competition selection", e)
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
        // 1. Update the Selectable List (for the Setup Screen checkboxes)
        _setupLeagues.update { currentList ->
            currentList.map { selectableItem ->
                if (selectableItem.item.leagueid == league.leagueid) {
                    selectableItem.copy(isSelected = isSelected)
                } else {
                    selectableItem
                }
            }
        }

        // 2. Update the main Setup object (for the Home Screen / Leagues Screen)
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

    fun saveChanges() {
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
    }

    fun onSetupDetailChanged(email: String, name: String) {
        // Get the current state value
        val currentSetup = _setup.value
        if (currentSetup == null) return

        // --- STEP 1: UPDATE THE UI STATE IMMEDIATELY ---
        // Create the updated object.
        val updatedSetup = currentSetup.copy(email = email, name = name)
        // Manually update the StateFlow. This is critical for TextField responsiveness.
        // This gives the UI an immediate new value to work with.
        _setup.value = updatedSetup
        Log.d("ViewModel_Update", "UI state updated immediately with name: $name, email: $email")

        // --- STEP 2: SAVE TO THE DATABASE IN THE BACKGROUND ---
        // Launch a separate coroutine to handle the slower database write.
        // This operation will no longer block the UI's state updates.
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertSetup(updatedSetup)
            Log.d("ViewModel_Update", "Database write completed in the background.")
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

        // 1. Mettre à jour l'état UI immédiatement
        val updatedSetup = currentSetup.copy(receive_email = receiveEmail)
        _setup.value = updatedSetup

        // 2. Persister dans la BD locale et sur le serveur
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.insertSetup(updatedSetup)
                setupRepository.updateUserPrivacy(receiveEmail)
                Log.d("ListSetupViewModel", "Privacy settings updated: $receiveEmail")
            } catch (e: Exception) {
                Log.e("ListSetupViewModel", "Failed to update privacy settings", e)
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

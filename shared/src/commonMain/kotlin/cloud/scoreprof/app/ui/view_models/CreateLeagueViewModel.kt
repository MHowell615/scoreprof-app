package cloud.scoreprof.app.ui.view_models

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.scoreprof.app.data.LeaguesRepository
import cloud.scoreprof.app.data.NotificationRepository
import cloud.scoreprof.app.domain.model.NotificationType
import cloud.scoreprof.app.domain.model.SendNotification
import cloud.scoreprof.app.data.SetupRepository
import cloud.scoreprof.app.domain.model.Competition
import cloud.scoreprof.app.domain.model.League
import cloud.scoreprof.app.domain.model.LeagueHeader
import cloud.scoreprof.app.domain.model.Setup
import cloud.scoreprof.app.domain.model.UserLeague
import cloud.scoreprof.app.domain.model.UserLeagueUsers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateLeagueViewModel(
    private val leaguesRepository: LeaguesRepository,
    private val setupRepository: SetupRepository,
    private val notificationRepository: NotificationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _leagueName = MutableStateFlow("")
    val leagueName: StateFlow<String> = _leagueName

    private val _selectedCompetition = MutableStateFlow<Competition?>(null)
    val selectedCompetition: StateFlow<Competition?> = _selectedCompetition
    private val _availableCompetitions = MutableStateFlow<List<Competition>>(emptyList())
    val availableCompetitions: StateFlow<List<Competition>> = _availableCompetitions

    private val _saveResult = MutableStateFlow<CreateLeagueResult>(CreateLeagueResult.Idle)
    val saveResult: StateFlow<CreateLeagueResult> = _saveResult

    private val _inviteEmailInput = MutableStateFlow("")
    val inviteEmailInput: StateFlow<String> = _inviteEmailInput

    private val _invitedEmails = MutableStateFlow<List<String>>(emptyList())
    val invitedEmails: StateFlow<List<String>> = _invitedEmails

    private val _inviteStatus = MutableStateFlow<String?>(null)
    val inviteStatus: StateFlow<String?> = _inviteStatus

    private val _setup = MutableStateFlow<Setup?>(null)
    val setup = _setup.asStateFlow()

    val userid: String? = savedStateHandle.get<String>("userid")
    val username: String = savedStateHandle.get<String>("username") ?: "Unknown User"
    val userEmail: String = savedStateHandle.get<String>("email") ?: ""

    init {
        viewModelScope.launch {
            userid?.let { id ->
                setupRepository.getSetup(id).collect { setupFromDb ->
                    _setup.value = setupFromDb
                }
            }
        }
    }

    fun onEmailAdded() {
        val email = _inviteEmailInput.value.trim()
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$".toRegex()
        
        if (!email.matches(emailRegex)) {
            _inviteStatus.value = "Please enter a valid email."
            return
        }
        if (_invitedEmails.value.contains(email)) {
            _inviteStatus.value = "This email has already been added."
            return
        }

        _invitedEmails.value = _invitedEmails.value + email
        _inviteEmailInput.value = ""
        _inviteStatus.value = "Email added"
    }

    fun onEmailRemoved(email: String) {
        _invitedEmails.value = _invitedEmails.value - email
    }

    fun onInviteEmailChanged(email: String) {
        _inviteEmailInput.value = email
    }

    fun onInviteStatusConsumed() {
        _inviteStatus.value = null
    }

    fun onResultConsumed() {
        _saveResult.value = CreateLeagueResult.Idle
    }

    fun onLeagueNameChanged(newName: String) {
        _leagueName.value = newName
        if (_saveResult.value is CreateLeagueResult.Error) {
            _saveResult.value = CreateLeagueResult.Idle
        }
    }

    fun setAvailableCompetitions(competitions: List<Competition>) {
        _availableCompetitions.value = competitions
    }

    fun onCompetitionSelected(competition: Competition) {
        if (_selectedCompetition.value == competition) {
            _selectedCompetition.value = null
        } else {
            _selectedCompetition.value = competition
        }
    }

    fun createLeague() {
        val currentLeagueName = _leagueName.value
        val currentSelectedCompetition = _selectedCompetition.value

        if (currentLeagueName.isBlank() || userid == null || currentSelectedCompetition == null) {
            _saveResult.value = CreateLeagueResult.Error("All fields are required.")
            return
        }

        viewModelScope.launch {
            _saveResult.value = CreateLeagueResult.Loading
            val ownerStatus = UserLeagueUsers(
                leagueid = currentLeagueName,
                owneruserid = userid,
                userid = userid,
                competitionid = currentSelectedCompetition.competitionid,
                leaguecode = "",
                invited = true,
                selected = true,
                invitestatus = "Accepted",
                state = "Active",
                email = userEmail
            )

            val allUserStatuses = listOf(ownerStatus)
            val userPayload = UserLeague(userleagueusers = allUserStatuses)

            val ownerStats = League(
                leagueid = currentLeagueName,
                owneruserid = userid,
                userid = userid,
                username = username
            )

            val headerPayload = LeagueHeader(
                leagueid = currentLeagueName,
                owneruserid = userid,
                competitionid = currentSelectedCompetition.competitionid,
                leagueusers = listOf(ownerStats)
            )

            try {
                val result = leaguesRepository.createNewLeague(
                    headerPayload,
                    userPayload,
                    userEmail
                )

                setupRepository.refreshSetupFromServer(userid)
                _saveResult.value = CreateLeagueResult.Success("League created", result.leagueCode)

            } catch (e: Exception) {
                _saveResult.value = CreateLeagueResult.Error("An unexpected error occurred. Please try again.")
            }
        }
    }

    sealed class CreateLeagueResult {
        data object Idle : CreateLeagueResult()
        data object Loading : CreateLeagueResult()
        data class Success(val message: String, val leagueCode: String) : CreateLeagueResult()
        data class Error(val message: String) : CreateLeagueResult()
    }
}

package cloud.scoreprof.app.ui.view_models

import android.content.Context
import android.util.Log
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
import cloud.scoreprof.app.ui.utils.UUIDSerializer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreateLeagueViewModel @Inject constructor(
    private val leaguesRepository: LeaguesRepository,
    private val setupRepository: SetupRepository,
    private val notificationRepository: NotificationRepository,
    @ApplicationContext private val context: Context, // Add this line
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

    // This holds the text currently in the email input field.
    private val _inviteEmailInput = MutableStateFlow("")
    val inviteEmailInput: StateFlow<String> = _inviteEmailInput

    // This new StateFlow will hold the temporary list of emails you want to invite.
    private val _invitedEmails = MutableStateFlow<List<String>>(emptyList())
    val invitedEmails: StateFlow<List<String>> = _invitedEmails

    // This state is for showing toast messages like "Email added" or "Invalid email".
    private val _inviteStatus = MutableStateFlow<String?>(null)
    val inviteStatus: StateFlow<String?> = _inviteStatus

    private val _setup = MutableStateFlow<Setup?>(null)
    val setup = _setup.asStateFlow()

    private val userIdString: String? = savedStateHandle.get<String>("userid")
    val userid: UUID? = try {
        userIdString?.let { UUID.fromString(it) }
    } catch (e: Exception) {
        null
    }
    val username: String = savedStateHandle.get<String>("username") ?: "Unknown User"
    val userEmail: String = savedStateHandle.get<String>("email") ?: ""

    init {
        // Automatically keep the local setup state in sync with the room db
        viewModelScope.launch {
            userid?.let { id ->
                setupRepository.getSetup(userid).collect { setupFromDb ->
                    _setup.value = setupFromDb
                }
            }
        }
    }
    fun onEmailAdded() {
        val email = _inviteEmailInput.value.trim()

        // --- Validation ---
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _inviteStatus.value = "Please enter a valid email."
            return
        }
        if (_invitedEmails.value.contains(email)) {
            _inviteStatus.value = "This email has already been added."
            return
        }

        // --- Update the state ---
        // Add the new email to the current list and update the StateFlow.
        _invitedEmails.value = _invitedEmails.value + email
        _inviteEmailInput.value = "" // Clear the input field after adding.
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
        // If we were showing a validation error, we can clear it now
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
        println("TEST: currentLeagueName = $currentLeagueName, currentSelectedCompetition = $currentSelectedCompetition")

        // --- Validation ---
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
                invited = true,
                selected = true,
                invitestatus = "Accepted",
                state = "Active",
                email = userEmail
            )

            val inviteeStatuses = _invitedEmails.value.map { email ->
                UserLeagueUsers(
                    userid = UUIDSerializer.EMPTY_UUID, // Shadow User trigger
                    leagueid = currentLeagueName,
                    owneruserid = userid,
                    competitionid = currentSelectedCompetition.competitionid,
                    invited = true,
                    selected = false,
                    invitestatus = "Pending",
                    state = "Active",
                    email = email
                )
            }

            val allUserStatuses = listOf(ownerStatus) + inviteeStatuses
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
                // STEP 1: Call the repository. It returns the new league ID, or 0 on conflict.
                leaguesRepository.createNewLeague(
                    headerPayload,
                    userPayload,
                    userEmail
                )

                setupRepository.refreshSetupFromServer(userid)

                sendInviteEmails(
                    leagueid = headerPayload.leagueid
                )

                _saveResult.value = CreateLeagueResult.Success("League created and invites sent!")

            } catch (e: Exception) {
                // This catches unexpected errors (server down, bad response format, etc.).
                _saveResult.value = CreateLeagueResult.Error("An unexpected error occurred. Please try again.")
                Log.e("CreateLeagueVM", "Error in createLeague flow", e)
            }
        }
    }

    private fun sendInviteEmails(leagueid: String) {
        val emailsToInvite = _invitedEmails.value
        if (emailsToInvite.isEmpty()) return

        emailsToInvite.forEach { email ->
            viewModelScope.launch {
                val notification =SendNotification(
                    email = email,
                    leagueid = leagueid,
                    isRead = false,
                    type = NotificationType.LEAGUE_INVITE
                )
                notificationRepository.sendNotification(notification)
                setupRepository.sendInviteEmail(email, username, leagueid, owneruserid = userid.toString())
            }
        }
    }

    sealed class CreateLeagueResult {
        object Idle : CreateLeagueResult() // The screen is waiting for user action
        object Loading : CreateLeagueResult() // The "Save" button was clicked, and we are saving
        data class Success(val message: String) : CreateLeagueResult() // Save was successful
        data class Error(val message: String) : CreateLeagueResult() // Save failed
    }
}
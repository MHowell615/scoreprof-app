package cloud.scoreprof.app.ui.view_models

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.scoreprof.app.data.LeaguesRepository
import cloud.scoreprof.app.data.NotificationRepository
import cloud.scoreprof.app.data.ScoreProfDao
import cloud.scoreprof.app.data.SetupRepository
import cloud.scoreprof.app.domain.model.Competition
import cloud.scoreprof.app.domain.model.LeagueHeader
import cloud.scoreprof.app.domain.model.NotificationType
import cloud.scoreprof.app.domain.model.SendNotification
import cloud.scoreprof.app.domain.model.UserLeague
import cloud.scoreprof.app.domain.model.UserLeagueUsers
import cloud.scoreprof.app.ui.utils.UUIDSerializer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditLeagueViewModel @Inject constructor(
    private val leaguesRepository: LeaguesRepository,
    private val setupRepository: SetupRepository,
    private val notificationRepository: NotificationRepository,
    private val dao: ScoreProfDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _leagueName = MutableStateFlow("")
    val leagueName: StateFlow<String> = _leagueName

    private var _leagueHeader = MutableStateFlow<LeagueHeader?>(null)
    var leagueHeader: StateFlow<LeagueHeader?> = _leagueHeader

    private val _leaguecode = MutableStateFlow("")
    val leaguecode: StateFlow<String> = _leaguecode.asStateFlow()

    val leagueid: String = savedStateHandle.get<String>("leagueid") ?: ""
    // Temporary debug line
    init {
        Log.d("EditLeagueVM", "Available Nav Args: ${savedStateHandle.keys()}")
    }

    private val ownerIdString: String = savedStateHandle.get<String>("owneruserid") ?: ""
    val owneruserid: UUID = try {
        UUID.fromString(ownerIdString)
    } catch (e: Exception) {
        UUIDSerializer.EMPTY_UUID // Or a default
    }

    private val _selectedCompetition = MutableStateFlow<Competition?>(null)
    val selectedCompetition: StateFlow<Competition?> = _selectedCompetition
    private val _availableCompetitions = MutableStateFlow<List<Competition>>(emptyList())
    val availableCompetitions: StateFlow<List<Competition>> = _availableCompetitions

    private val _saveResult = MutableStateFlow<EditLeagueResult>(EditLeagueResult.Idle)
    val saveResult: StateFlow<EditLeagueResult> = _saveResult

    private val _inviteEmailInput = MutableStateFlow("")
    val inviteEmailInput: StateFlow<String> = _inviteEmailInput
    private val _invitedEmails = MutableStateFlow<List<String>>(emptyList())
    val invitedEmails: StateFlow<List<String>> = _invitedEmails

    private val _inviteStatus = MutableStateFlow<String?>(null)
    val inviteStatus: StateFlow<String?> = _inviteStatus

    private val _userLeagueUsers = MutableStateFlow<List<UserLeagueUsers>>(emptyList())
    val userLeagueUsers: StateFlow<List<UserLeagueUsers>> = _userLeagueUsers

    private val useridString: String = requireNotNull(savedStateHandle.get<String>("userid")) {
        "User ID is required to create a league"
    }
    val userid: UUID = UUID.fromString(useridString)
    val username: String = savedStateHandle.get<String>("username") ?: ""
    val ownerUserEmail: String = savedStateHandle.get<String>("email") ?: ""

    init {
        if (owneruserid != UUIDSerializer.EMPTY_UUID && leagueid.isNotEmpty()) {
            getLeagueDetails(leagueid, owneruserid)
        } else {
            Log.e("EditLeagueVM", "Invalid Navigation Arguments: leagueid=$leagueid, owner=$ownerIdString")
            _saveResult.value = EditLeagueResult.Error("Navigation error: Missing league identity.")
        }
    }

    fun fetchLeagueCode(leagueId: String, ownerId: UUID) {
        val users = _userLeagueUsers.value
        if (users.isNotEmpty()) {
            val code = users.firstOrNull()?.leaguecode ?: ""
            _leaguecode.value = code
            Log.d("EditLeagueVM", "Extracted league code from users list: $code")
        }
    }

    fun getLeagueDetails(leagueid: String, owneruserid: UUID) {
        viewModelScope.launch {
            try {
                println("TEST: (EditLeagueViewModel) getLeagueDetails() called for $leagueid, $owneruserid")
                // 1. Fetch the data from the repository
                val userStatusList = leaguesRepository.getLeagueUserStatuses(leagueid, owneruserid.toString())
                println("TEST: (EditLeagueViewModel) getLeagueDetails() returned $userStatusList")

                val activeUsers = userStatusList.filter {
                    it.state?.lowercase() != "deleted" && it.invitestatus?.lowercase() != "deleted"
                }

                _userLeagueUsers.value = activeUsers
                _leaguecode.value = activeUsers.firstOrNull { it.leaguecode?.isNotEmpty() == true }?.leaguecode ?: ""

                // 3. Extract just the "Other" invited emails (excluding the owner)
                // This is what populates the "Invites" list on your UI
                val inviteesOnly = activeUsers
                    .filter { it.userid != owneruserid } // Don't show the owner in the invite list
                    .map { it.email }
                    .filter { it.isNotBlank() }

                if (inviteesOnly.isNotEmpty()) {
                    _invitedEmails.value = inviteesOnly
                }

                // 4. Update Header and Selection logic (already existing)
                val header = leaguesRepository.getEditLeague(leagueid, owneruserid)

                println("TEST: (EditLeagueViewModel) getLeagueDetails() header = $header")

                header?.let {
                    _leagueHeader.value = it
                    _leagueName.value = it.leagueid
                    syncSelectedCompetition()
                }

                syncSelectedCompetition()

            } catch (e: Exception) {
                // Handle error
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
        _saveResult.value = EditLeagueResult.Idle
    }

    fun onLeagueNameChanged(newName: String) {
        _leagueName.value = newName
        // If we were showing a validation error, we can clear it now
        if (_saveResult.value is EditLeagueResult.Error) {
            _saveResult.value = EditLeagueResult.Idle
        }

        fun refreshDetails() {
            getLeagueDetails(leagueid, owneruserid)
        }
    }

    fun setAvailableCompetitions(competitions: List<Competition>) {
        if (competitions.isNotEmpty()) {
            _availableCompetitions.value = competitions
            // This is the "Safety Trigger" - sync as soon as the list arrives
            syncSelectedCompetition()
        }
    }

    private fun syncSelectedCompetition() {
        val header = _leagueHeader.value
        val list = _availableCompetitions.value

        // Only sync if we have both the header AND the full list of competitions
        if (header != null && list.isNotEmpty()) {
            val found = list.find {
                it.competitionid.trim().equals(header.competitionid?.trim(), ignoreCase = true)
            }
            _selectedCompetition.value = found

            // Debug log to verify if it found a match
            if (found == null) {
                Log.e("EditLeagueVM", "Could not find competition matching: ${header.competitionid} in list of ${list.size}")
            } else {
                Log.d("EditLeagueVM", "Sync successful: Selected ${found.competitionid}")
            }
        }
    }

    fun onCompetitionSelected(competition: Competition?) {
        // Toggle logic: if clicking the same one, deselect it
        if (_selectedCompetition.value?.competitionid == competition?.competitionid) {
            _selectedCompetition.value = null
        } else {
            _selectedCompetition.value = competition
        }
    }

    fun saveLeagueDetails() {

        if (owneruserid == UUIDSerializer.EMPTY_UUID || userid == UUIDSerializer.EMPTY_UUID) {
            _saveResult.value = EditLeagueResult.Error("Security error: Invalid user session.")
            return
        }

        val currentLeagueName = _leagueName.value
        val currentSelectedCompetition = _selectedCompetition.value
        val currentInviteesInUI = _invitedEmails.value
        val existingHeader = _leagueHeader.value
        val leaguecode = _leaguecode.value
        Log.d("EditLeagueVM", "VALIDATION CHECK: name='$currentLeagueName', userid=$userid, comp=${currentSelectedCompetition?.competitionid}")
println("TEST: (EditLeagueViewModel) saveLeagueDetails() called for $leagueid, $owneruserid")
println("TEST: (EditLeagueViewModel) saveLeagueDetails() existingHeader = $existingHeader")

        // --- Validation ---
        if (currentLeagueName.isBlank() || userid == null || currentSelectedCompetition == null) {
            _saveResult.value = EditLeagueResult.Error("All fields are required.")
            return
        }

        viewModelScope.launch {
            _saveResult.value = EditLeagueResult.Loading
            // need to somehow build a list of League() with each user added to the email invites
            try {
                // STEP 1: Fetch the "Status" info from the server that we don't have locally
                // This returns the current invited/selected/invitestatus flags for all users in this league
                val remoteUserStatuses =
                    leaguesRepository.getLeagueUserStatuses(leagueid, owneruserid.toString())

                // STEP 2: Build the LeagueUsers list (Stats)
                // We map through the existing header to preserve points/matches
                val baseLeagueUsers = existingHeader?.leagueusers?.map { user ->
                    user.copy(leagueid = currentLeagueName)
                } ?: emptyList()

                // Ensure the owner is in the stats list for the new league
                val isOwnerInStats = baseLeagueUsers.any { it.userid == userid }
                val leagueUsers = if (!isOwnerInStats) {
                    baseLeagueUsers + cloud.scoreprof.app.domain.model.League(
                        leagueid = currentLeagueName,
                        owneruserid = owneruserid,
                        userid = userid,
                        username = username
                    )
                } else {
                    baseLeagueUsers
                }

                // STEP 3: Build the UserLeague payload (Invited/Selected flags)
                // We merge the server's known status with our current list
                val existingMembers = remoteUserStatuses.filter { status ->
                    // Keep the row if it's the owner OR if the email is still in our UI list
                    status.userid == userid || currentInviteesInUI.any {
                        it.equals(
                            status.email,
                            ignoreCase = true
                        )
                    }
                }.map { status ->
                    status.copy(
                        leagueid = currentLeagueName,
                        competitionid = currentSelectedCompetition.competitionid,
                        userid = status.userid,
                        selected = if (status.userid == userid) true else status.selected
                    )
                }
                val newInvitees = currentInviteesInUI.filter { email ->
                    remoteUserStatuses.none { it.email.equals(email, ignoreCase = true) }
                }.map { email ->
                    UserLeagueUsers(
                        userid = UUIDSerializer.EMPTY_UUID, // Crucial: This triggers the SQL 'Shadow User' logic
                        leagueid = currentLeagueName,
                        owneruserid = owneruserid,
                        competitionid = currentSelectedCompetition.competitionid,
                        leaguecode = leaguecode,
                        invited = true,
                        selected = false,
                        invitestatus = "Pending",
                        state = "Active",
                        email = email
                    )
                }

                val isOwnerInList = existingMembers.any { it.userid == userid }
                val ownerRecord = if (!isOwnerInList) {
                    listOf(
                        UserLeagueUsers(
                            userid = userid, // Use the current user's ID
                            leagueid = currentLeagueName,
                            owneruserid = owneruserid,
                            competitionid = currentSelectedCompetition.competitionid,
                            leaguecode = leaguecode,
                            invited = true,
                            selected = true,
                            invitestatus = "Accepted",
                            state = "Active",
                            email = ownerUserEmail
                        )
                    )
                } else emptyList()

                val finalUserList = UserLeague(existingMembers + newInvitees + ownerRecord)
                val headerPayload = LeagueHeader(
                    leagueid = currentLeagueName,
                    owneruserid = owneruserid,
                    competitionid = currentSelectedCompetition.competitionid,
                    leagueusers = leagueUsers
                )


                leaguesRepository.saveEditLeague(
                    originalLeagueId = leagueid,
                    leagueHeader = headerPayload,
                    userLeague = finalUserList,
                    userEmail = ownerUserEmail
                )

                setupRepository.refreshSetupFromServer(userid)


                sendInviteEmails(
                    leagueid = headerPayload.leagueid
                )

                _saveResult.value = EditLeagueResult.Success("League updated and invites sent!")
            } catch (e: Exception) {
                // This catches unexpected errors (server down, bad response format, etc.).
                _saveResult.value =
                    EditLeagueResult.Error("An unexpected error occurred. Please try again.")
                Log.e("CreateLeagueVM", "Error in createLeague flow", e)
            }
        }
    }



    private fun sendInviteEmails(leagueid: String) {
        val emailsToInvite = _invitedEmails.value
        if (emailsToInvite.isEmpty()) return

        emailsToInvite.forEach { email ->
            viewModelScope.launch {
                val notification = SendNotification(
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

    suspend fun onEvent(event: LeagueEvent) {
        when (event) {
            is LeagueEvent.GetLeagueDetails -> {
                getLeagueDetails(event.leagueid, event.owneruserid)
            }

            is LeagueEvent.SaveLeagueDetails -> {
                saveLeagueDetails()
            }
        }
    }

    sealed class EditLeagueResult {
        object Idle : EditLeagueResult() // The screen is waiting for user action
        object Loading : EditLeagueResult() // The "Save" button was clicked, and we are saving
        data class Success(val message: String) : EditLeagueResult() // Save was successful
        data class Error(val message: String) : EditLeagueResult() // Save failed
    }

    sealed class LeagueEvent {
        data class GetLeagueDetails(
            val leagueid: String,
            val owneruserid: UUID
        ) : LeagueEvent()

        data class SaveLeagueDetails(
            val leagueid: String,
            val owneruserid: UUID
        ) : LeagueEvent()
    }
}


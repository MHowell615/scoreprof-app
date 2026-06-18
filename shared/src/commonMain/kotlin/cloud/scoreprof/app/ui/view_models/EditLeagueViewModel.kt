package cloud.scoreprof.app.ui.view_models

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditLeagueViewModel(
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
    private val owneruserid: String = savedStateHandle.get<String>("owneruserid") ?: ""

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

    private val userid: String = savedStateHandle.get<String>("userid") ?: ""
    val username: String = savedStateHandle.get<String>("username") ?: ""
    val ownerUserEmail: String = savedStateHandle.get<String>("email") ?: ""

    init {
        if (owneruserid.isNotEmpty() && leagueid.isNotEmpty()) {
            getLeagueDetails(leagueid, owneruserid)
        } else {
            _saveResult.value = EditLeagueResult.Error("Navigation error: Missing league identity.")
        }
    }

    fun getLeagueDetails(leagueid: String, owneruserid: String) {
        viewModelScope.launch {
            try {
                val userStatusList = leaguesRepository.getLeagueUserStatuses(leagueid, owneruserid)
                val activeUsers = userStatusList.filter {
                    it.state?.lowercase() != "deleted" && it.invitestatus?.lowercase() != "deleted"
                }

                _userLeagueUsers.value = activeUsers
                _leaguecode.value = activeUsers.firstOrNull { it.leaguecode?.isNotEmpty() == true }?.leaguecode ?: ""

                val inviteesOnly = activeUsers
                    .filter { it.userid != owneruserid }
                    .map { it.email }
                    .filter { it.isNotBlank() }

                if (inviteesOnly.isNotEmpty()) {
                    _invitedEmails.value = inviteesOnly
                }

                val header = leaguesRepository.getEditLeague(leagueid, owneruserid)
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
        _saveResult.value = EditLeagueResult.Idle
    }

    fun onLeagueNameChanged(newName: String) {
        _leagueName.value = newName
        if (_saveResult.value is EditLeagueResult.Error) {
            _saveResult.value = EditLeagueResult.Idle
        }
    }

    fun setAvailableCompetitions(competitions: List<Competition>) {
        if (competitions.isNotEmpty()) {
            _availableCompetitions.value = competitions
            syncSelectedCompetition()
        }
    }

    private fun syncSelectedCompetition() {
        val header = _leagueHeader.value
        val list = _availableCompetitions.value

        if (header != null && list.isNotEmpty()) {
            val found = list.find {
                it.competitionid.trim().equals(header.competitionid?.trim(), ignoreCase = true)
            }
            _selectedCompetition.value = found
        }
    }

    fun onCompetitionSelected(competition: Competition?) {
        if (_selectedCompetition.value?.competitionid == competition?.competitionid) {
            _selectedCompetition.value = null
        } else {
            _selectedCompetition.value = competition
        }
    }

    fun saveLeagueDetails() {
        val currentLeagueName = _leagueName.value
        val currentSelectedCompetition = _selectedCompetition.value
        val currentInviteesInUI = _invitedEmails.value
        val existingHeader = _leagueHeader.value
        val leaguecode = _leaguecode.value

        if (currentLeagueName.isBlank() || userid.isEmpty() || currentSelectedCompetition == null) {
            _saveResult.value = EditLeagueResult.Error("All fields are required.")
            return
        }

        viewModelScope.launch {
            _saveResult.value = EditLeagueResult.Loading
            try {
                val remoteUserStatuses = leaguesRepository.getLeagueUserStatuses(leagueid, owneruserid)
                val baseLeagueUsers = existingHeader?.leagueusers?.map { user ->
                    user.copy(leagueid = currentLeagueName)
                } ?: emptyList()

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

                val existingMembers = remoteUserStatuses.filter { status ->
                    status.userid == userid || currentInviteesInUI.any { it.equals(status.email, ignoreCase = true) }
                }.map { status ->
                    status.copy(
                        leagueid = currentLeagueName,
                        competitionid = currentSelectedCompetition.competitionid,
                        selected = if (status.userid == userid) true else status.selected
                    )
                }
                
                val newInvitees = currentInviteesInUI.filter { email ->
                    remoteUserStatuses.none { it.email.equals(email, ignoreCase = true) }
                }.map { email ->
                    UserLeagueUsers(
                        userid = "00000000-0000-0000-0000-000000000000",
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
                            userid = userid,
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

                leaguesRepository.saveEditLeague(leagueid, headerPayload, finalUserList, ownerUserEmail)
                setupRepository.refreshSetupFromServer(userid)
                sendInviteEmails(headerPayload.leagueid)

                _saveResult.value = EditLeagueResult.Success("League updated and invites sent!")
            } catch (e: Exception) {
                _saveResult.value = EditLeagueResult.Error("An unexpected error occurred.")
            }
        }
    }

    private fun sendInviteEmails(leagueid: String) {
        val emailsToInvite = _invitedEmails.value
        emailsToInvite.forEach { email ->
            viewModelScope.launch {
                val notification = SendNotification(
                    email = email,
                    leagueid = leagueid,
                    isRead = false,
                    type = NotificationType.LEAGUE_INVITE
                )
                notificationRepository.sendNotification(notification)
                //setupRepository.sendInviteEmail(email, username, leagueid, userid, "Invite", "Message")
            }
        }
    }

    suspend fun onEvent(event: LeagueEvent) {
        when (event) {
            is LeagueEvent.GetLeagueDetails -> getLeagueDetails(event.leagueid, event.owneruserid)
            is LeagueEvent.SaveLeagueDetails -> saveLeagueDetails()
        }
    }

    sealed class EditLeagueResult {
        data object Idle : EditLeagueResult()
        data object Loading : EditLeagueResult()
        data class Success(val message: String) : EditLeagueResult()
        data class Error(val message: String) : EditLeagueResult()
    }

    sealed class LeagueEvent {
        data class GetLeagueDetails(val leagueid: String, val owneruserid: String) : LeagueEvent()
        data class SaveLeagueDetails(val leagueid: String, val owneruserid: String) : LeagueEvent()
    }
}

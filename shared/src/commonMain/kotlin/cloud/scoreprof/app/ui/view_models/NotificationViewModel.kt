package cloud.scoreprof.app.ui.view_models

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.scoreprof.app.data.NotificationRepository
import cloud.scoreprof.app.data.ScoreProfDao
import cloud.scoreprof.app.data.local.TokenManager
import cloud.scoreprof.app.domain.model.AppNotification
import cloud.scoreprof.app.domain.model.NotificationType
import cloud.scoreprof.app.domain.model.SendNotification
import cloud.scoreprof.app.Platform
import cloud.scoreprof.app.data.SetupRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val notificationRepository: NotificationRepository,
    private val setupRepository: SetupRepository,
    private val savedStateHandle: SavedStateHandle,
    private val tokenManager: TokenManager,
    private val dao: ScoreProfDao,
    private val platform: Platform
) : ViewModel() {
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications

    private val _currentNotification = MutableStateFlow<AppNotification?>(null)
    val currentNotification: StateFlow<AppNotification?> = _currentNotification

    val token = tokenManager.getToken() ?: ""

    init {
        viewModelScope.launch {
            val id = savedStateHandle.get<Int>("id")
            val email = savedStateHandle.get<String>("email")

            if (id != null && email != null) {
                _notifications.value = _notifications.value.map {
                    if (it.notificationid == id) {
                        if (!it.isread) {
                            markAsRead(email, id)
                        }
                        it.copy(isread = true)
                    } else it
                }
            }
        }
    }

    val unreadCount = notifications.map { list ->
        list.count { !it.isread }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun loadNotificationById(id: Int) {
        viewModelScope.launch {
            _currentNotification.value = dao.getAppNotification(id)
        }
    }

    fun sendNotification(inviteeEmail: String, leagueid: String) {
        val notification = SendNotification(
            email = inviteeEmail,
            leagueid = leagueid,
            isRead = false,
            type = NotificationType.LEAGUE_INVITE
        )

        viewModelScope.launch {
            notificationRepository.sendNotification(notification)
        }
    }

    fun markAsRead(email: String, id: Int) {
        viewModelScope.launch {
            dao.markNotificationAsReadLocal(id)
            notificationRepository.markAsReadNotification(email, id)
        }
    }

    fun markAllAsRead() {
        _notifications.value = _notifications.value.map { it.copy(isread = true) }
    }

    fun loadNotifications(email: String) {
        viewModelScope.launch {
            try {
                val fetched = notificationRepository.fetchNotifications(token, email)
                
                // Show system notification for new items if enabled
                setupRepository.getSetup(tokenManager.getUserId() ?: "").collectLatest { setup ->
                    if (setup?.receive_notifications == true) {
                        val lastNotifiedId = tokenManager.getLastNotifiedId()
                        val newNotifications = fetched.filter { 
                            !it.isread && 
                            it.notificationid > lastNotifiedId &&
                            _notifications.value.none { old -> old.notificationid == it.notificationid } 
                        }
                        
                        if (newNotifications.isNotEmpty()) {
                            newNotifications.forEach { 
                                platform.showSystemNotification(it.title ?: "ScoreProf", it.message ?: "")
                            }
                            tokenManager.saveLastNotifiedId(newNotifications.maxOf { it.notificationid })
                        }
                    }
                }

                _notifications.value = fetched
            } catch (e: Exception) {
                println("Error loading notifications: ${e.message}")
            }
        }
    }

    fun acceptJoinRequest(notification: AppNotification) {
        viewModelScope.launch {
            try {
                val success = notificationRepository.acceptJoinRequest(
                    token = token,
                    leagueId = notification.leagueid ?: "",
                    joinerId = notification.joinerid ?: ""
                )

                if (success) {
                    markAsRead(notification.email, notification.notificationid)
                    _notifications.value = _notifications.value.filter {
                        it.notificationid != notification.notificationid
                    }
                }
            } catch (e: Exception) {
                println("Error accepting join request: ${e.message}")
            }
        }
    }

    fun declineJoinRequest(notification: AppNotification) {
        viewModelScope.launch {
            markAsRead(notification.email, notification.notificationid)
        }
    }
}

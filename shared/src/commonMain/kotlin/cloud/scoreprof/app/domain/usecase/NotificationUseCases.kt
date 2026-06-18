package cloud.scoreprof.app.domain.usecase

data class NotificationUseCases (
    val sendNotifications: SendNotificationsUseCase,
    val markAsRead: MarkAsReadUseCase
)
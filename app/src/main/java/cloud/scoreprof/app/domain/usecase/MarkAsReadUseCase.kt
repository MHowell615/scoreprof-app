package cloud.scoreprof.app.domain.usecase

import cloud.scoreprof.app.data.NotificationRepository

class MarkAsReadUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke(email: String, id: Int) {
        repository.markAsReadNotification(email, id)
    }
}
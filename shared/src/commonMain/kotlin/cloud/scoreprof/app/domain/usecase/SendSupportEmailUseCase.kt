package cloud.scoreprof.app.domain.usecase

import cloud.scoreprof.app.data.SetupRepository

class SendSupportEmailUseCase(private val repository: SetupRepository) {
    suspend operator fun invoke(
        category: String,
        subject: String,
        details: String
    ): Boolean {
        return repository.sendSupportMessage(category, subject, details)
    }
}

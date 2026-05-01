package cloud.scoreprof.app.domain.usecase

import cloud.scoreprof.app.data.SetupRepository

class SendSupportEmailUseCase(private val repository: SetupRepository) {
    suspend operator fun invoke(
        userEmail: String,
        category: String,
        subject: String,
        description: String
    ): Boolean {
        return repository.sendSupportEmail(userEmail, category, subject, description)
    }
}
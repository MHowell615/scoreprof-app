package cloud.scoreprof.app.ui.view_models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.scoreprof.app.data.VersionRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VersionViewModel(
    private val versionRepository: VersionRepository
) : ViewModel() {
    val isUpdateRequired: StateFlow<Boolean> = versionRepository.isUpdateRequired
    val updateUrl: StateFlow<String> = versionRepository.updateUrl

    init {
        viewModelScope.launch {
            checkAppVersion()
        }
    }
    suspend fun checkAppVersion() {
        versionRepository.checkAppVersion()
    }
}

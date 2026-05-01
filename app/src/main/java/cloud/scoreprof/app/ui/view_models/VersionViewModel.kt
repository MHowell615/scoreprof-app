package cloud.scoreprof.app.ui.view_models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.scoreprof.app.data.VersionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VersionViewModel @Inject constructor(
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
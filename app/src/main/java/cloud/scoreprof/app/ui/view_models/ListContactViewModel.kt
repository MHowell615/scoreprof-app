package cloud.scoreprof.app.ui.view_models

import android.app.Application
import androidx.lifecycle.ViewModel
import cloud.scoreprof.app.data.ScoreProfDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ListContactViewModel @Inject constructor(
    private val application: Application,
    private val dao: ScoreProfDao
) : ViewModel() {


}
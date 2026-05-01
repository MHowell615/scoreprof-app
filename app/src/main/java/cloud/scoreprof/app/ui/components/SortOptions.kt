package cloud.scoreprof.app.ui.components

import cloud.scoreprof.app.ui.view_models.MatchVM

sealed class SortOrder()
data object SortByKickOffAsc : SortOrder()

data class NotesState(
    val matches: List<MatchVM> = emptyList(),
    val matchOrder: SortOrder = SortByKickOffAsc,
)





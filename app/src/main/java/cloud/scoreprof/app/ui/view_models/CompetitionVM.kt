package cloud.scoreprof.app.ui.view_models

import cloud.scoreprof.app.domain.model.Competition

data class CompetitionVM(
    val id: Int,
    val competitionid: String,
    val name: String,
    val isSelected: Boolean,
) {
    companion object {
        fun fromModel(entity: Competition, isSelected: Boolean): CompetitionVM {
            return CompetitionVM(
                id = entity.id,
                competitionid = entity.competitionid,
                name = entity.name,
                isSelected = isSelected
            )
        }
    }
}


package cloud.scoreprof.app.domain.usecase

data class MatchesUseCases(
    val getMatches: GetMatchesUseCase,
    val loadAndCacheMatches: LoadAndCacheMatchesUseCase,
    val updatePredictionInDb: UpdatePredictionInDbUseCase,
    val hasMatches: HasMatchesUseCase
)

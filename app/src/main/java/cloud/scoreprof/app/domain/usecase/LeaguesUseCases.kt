package cloud.scoreprof.app.domain.usecase

data class LeaguesUseCases(
    val getLeagues: GetLeaguesUseCase,
    val getLeagueTable: GetLeagueTableUseCase,
    val createLeague: CreateLeagueUseCase
)
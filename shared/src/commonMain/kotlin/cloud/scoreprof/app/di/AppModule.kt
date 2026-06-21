package cloud.scoreprof.app.di

import cloud.scoreprof.app.data.*
import cloud.scoreprof.app.data.local.TokenManager
import cloud.scoreprof.app.domain.usecase.*
import cloud.scoreprof.app.ui.view_models.*
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    // Services
    single { TokenManager(get()) }
    
    // Repositories
    single<SetupRepository> { SetupRepositoryImpl(get(), get(), get(), get(), "YOUR_SPROF_AUTH_KEY") }
    single<VersionRepository> { VersionRepositoryImpl(get(), get()) }
    single<LeaguesRepository> { LeaguesRepositoryImpl(get(), get(), get(), get(), get()) }
    single<MatchRepository> { MatchRepositoryImpl(get(), get(), get(), get()) }
    single<NotificationRepository> { NotificationRepositoryImpl(get(), get(), get()) }
    single<LanguageRepository> { LanguageRepositoryImpl(get(), get()) }
    single<PredictionUpdateRepository> { PredictionUpdateRepositoryImpl(get(), get()) }

    // UseCases
    singleOf(::GetSetupUseCase)
    singleOf(::LoadAndCacheSetupUseCase)
    singleOf(::UpdateUserCompetitionUseCase)
    singleOf(::SendSupportEmailUseCase)
    singleOf(::UpdateAllUserCompetitionsCase)
    single { SetupUseCases(get(), get(), get(), get(), get()) }

    singleOf(::GetLeaguesUseCase)
    singleOf(::GetLeagueTableUseCase)
    singleOf(::CreateLeagueUseCase)
    single { LeaguesUseCases(get(), get(), get()) }

    singleOf(::GetMatchesUseCase)
    singleOf(::LoadAndCacheMatchesUseCase)
    singleOf(::UpdatePredictionInDbUseCase)
    singleOf(::HasMatchesUseCase)
    single { MatchesUseCases(get(), get(), get(), get()) }

    singleOf(::GetLanguagesUseCase)
    singleOf(::LoadAndCacheLanguagesUseCase)
    single { LanguagesUseCases(get(), get()) }

    // ViewModels
    viewModelOf(::LoginViewModel)
    viewModelOf(::VersionViewModel)
    viewModelOf(::ListSetupViewModel)
    viewModelOf(::ListMatchesViewModel)
    viewModelOf(::ListLeaguesViewModel)
    viewModelOf(::ListLeagueViewModel)
    viewModelOf(::NotificationViewModel)
    viewModelOf(::CreateLeagueViewModel)
    viewModelOf(::EditLeagueViewModel)
    viewModelOf(::ListContactViewModel)
    viewModelOf(::ListHelpViewModel)
}

package cloud.scoreprof.app.di

import android.content.ContentValues.TAG
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.room.Room
import cloud.scoreprof.app.BuildConfig
import cloud.scoreprof.app.data.BillingManager
import cloud.scoreprof.app.data.LanguageRepository
import cloud.scoreprof.app.data.LanguageRepositoryImpl
import cloud.scoreprof.app.data.LeaguesRepository
import cloud.scoreprof.app.data.LeaguesRepositoryImpl
import cloud.scoreprof.app.data.MatchRepository
import cloud.scoreprof.app.data.MatchRepositoryImpl
import cloud.scoreprof.app.data.NotificationRepository
import cloud.scoreprof.app.data.NotificationRepositoryImpl
import cloud.scoreprof.app.data.ScoreProfDao
import cloud.scoreprof.app.data.ScoreProfDatabase
import cloud.scoreprof.app.data.PredictionUpdateRepository
import cloud.scoreprof.app.data.PredictionUpdateRepositoryImpl
import cloud.scoreprof.app.data.SetupRepository
import cloud.scoreprof.app.data.SetupRepositoryImpl
import cloud.scoreprof.app.data.VersionRepository
import cloud.scoreprof.app.data.VersionRepositoryImpl
import cloud.scoreprof.app.data.local.TokenManager
import cloud.scoreprof.app.domain.usecase.CreateLeagueUseCase
import cloud.scoreprof.app.domain.usecase.GetLanguagesUseCase
import cloud.scoreprof.app.domain.usecase.GetLeagueTableUseCase
import cloud.scoreprof.app.domain.usecase.GetLeaguesUseCase
import cloud.scoreprof.app.domain.usecase.GetMatchesUseCase
import cloud.scoreprof.app.domain.usecase.GetSetupUseCase
import cloud.scoreprof.app.domain.usecase.HasMatchesUseCase
import cloud.scoreprof.app.domain.usecase.LanguagesUseCases
import cloud.scoreprof.app.domain.usecase.LeaguesUseCases
import cloud.scoreprof.app.domain.usecase.LoadAndCacheLanguagesUseCase
import cloud.scoreprof.app.domain.usecase.LoadAndCacheMatchesUseCase
import cloud.scoreprof.app.domain.usecase.LoadAndCacheSetupUseCase
import cloud.scoreprof.app.domain.usecase.MatchesUseCases
import cloud.scoreprof.app.domain.usecase.SendSupportEmailUseCase
import cloud.scoreprof.app.domain.usecase.SetupUseCases
import cloud.scoreprof.app.domain.usecase.UpdatePredictionInDbUseCase
import cloud.scoreprof.app.domain.usecase.UpdateUserCompetitionUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseAndUseCaseModule {

    @Provides
    @Singleton
    fun provideScoreProfDatabase(
        @ApplicationContext context: Context,
        prefs: SharedPreferences
    ): ScoreProfDatabase {
        val currentVersion = BuildConfig.VERSION_CODE
        val lastVersion = prefs.getInt("last_run_version", -1)

        if (lastVersion != currentVersion) {
            prefs.edit().clear().apply()
            context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            context.deleteDatabase(ScoreProfDatabase.DATABASE_NAME)
            prefs.edit().putInt("last_run_version", currentVersion).apply()
        }

        return Room.databaseBuilder(
            context,
            ScoreProfDatabase::class.java,
            ScoreProfDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideScoreProfDao(database: ScoreProfDatabase): ScoreProfDao {
        return database.scoreProfDao()
    }

    @Provides
    @Singleton
    fun provideMatchesUseCases(
        repository: MatchRepository,
        tokenManager: TokenManager
    ): MatchesUseCases {
        val userId = tokenManager.getUserId() ?: ""
        return MatchesUseCases(
            getMatches = GetMatchesUseCase(repository),
            loadAndCacheMatches = LoadAndCacheMatchesUseCase(repository, userId),
            updatePredictionInDb = UpdatePredictionInDbUseCase(repository),
            hasMatches = HasMatchesUseCase(repository)
        )
    }

    @Provides
    @Singleton
    fun provideLanguagesUseCases(repository: LanguageRepository): LanguagesUseCases {
        return LanguagesUseCases(
            getLanguages = GetLanguagesUseCase(repository),
            loadAndCacheLanguages = LoadAndCacheLanguagesUseCase(repository)
        )
    }

    @Provides
    @Singleton
    fun provideSetupUseCases(repository: SetupRepository): SetupUseCases {
        return SetupUseCases(
            getSetup = GetSetupUseCase(repository),
            loadAndCacheSetup = LoadAndCacheSetupUseCase(repository),
            updateUserCompetition = UpdateUserCompetitionUseCase(repository),
            sendSupportEmail = SendSupportEmailUseCase(repository)
        )
    }

    @Provides
    @Singleton
    fun provideLeaguesUseCases(repository: LeaguesRepository): LeaguesUseCases {
        return LeaguesUseCases(
            getLeagues = GetLeaguesUseCase(repository),
            getLeagueTable = GetLeagueTableUseCase(repository),
            createLeague = CreateLeagueUseCase(repository)
        )
    }

    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
        return TokenManager(context)
    }

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
            encodeDefaults = true
        }
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("scoreprof_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideBillingManager(@ApplicationContext context: Context): BillingManager {
        return BillingManager(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMatchRepository(
        matchRepositoryImpl: MatchRepositoryImpl
    ): MatchRepository

    @Binds
    @Singleton
    abstract fun bindPredictionUpdateRepository(
        predictionUpdateRepositoryImpl: PredictionUpdateRepositoryImpl
    ): PredictionUpdateRepository

    @Binds
    @Singleton
    abstract fun bindLanguageRepository(
        LanguageRepositoryImpl: LanguageRepositoryImpl
    ): LanguageRepository

    @Binds
    @Singleton
    abstract fun bindSetupRepository(
        SetupRepositoryImpl: SetupRepositoryImpl
    ): SetupRepository

    @Binds
    @Singleton
    abstract fun bindLeaguesRepository(
        LeaguesRepositoryImpl: LeaguesRepositoryImpl
    ): LeaguesRepository

    @Binds
    @Singleton
    abstract fun bindVersionRepository(
        VersionRepositoryImpl: VersionRepositoryImpl
    ): VersionRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        NotificationRepositoryImpl: NotificationRepositoryImpl
    ): NotificationRepository
}

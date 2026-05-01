package cloud.scoreprof.app.di

import android.content.ContentValues.TAG
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.room.Room
import cloud.scoreprof.app.BuildConfig
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

/**
 * This module contains all the @Provides functions.
 * These are functions that have a body and manually create instances of objects.
 * Since it contains no abstract methods, it can be an 'object'.
 */
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

        Log.d("SPROF_DEBUG", "Database Init - Current: $currentVersion, Last Stored: $lastVersion")

        // IF THE VERSION CHANGED, WIPE EVERYTHING
        if (lastVersion != currentVersion) {
            println("SPROF_DEBUG: Version change detected ($lastVersion -> $currentVersion). Wiping local cache.")

            // 1. Clear SharedPreferences (except critical things like login if you want)
            // Note: If you want to keep them logged in, don't clear the token
            //prefs.edit().remove("cached_leagues").apply()
            prefs.edit().clear().apply()

            // 2. Clear TokenManager
            context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE).edit().clear().apply()

            // 3. Delete the actual database file to force a clean reconstruction
            context.deleteDatabase(ScoreProfDatabase.DATABASE_NAME)

            // 4. Save the new version code
            prefs.edit().putInt("last_run_version", currentVersion).apply()
        }

        Log.d(TAG, "Hilt is calling: provideScoreProfDatabase")
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
        Log.d(TAG, "Hilt is calling: provideScoreProfDao")
        return database.scoreProfDao()
    }

    @Provides
    @Singleton
    fun provideMatchesUseCases(
        repository: MatchRepository,
        tokenManager: TokenManager
    ): MatchesUseCases {
        Log.d(TAG, "Hilt is calling: provideMatchesUseCases")
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
        Log.d(TAG, "Hilt is calling: provideLanguagesUseCases")
        return LanguagesUseCases(
            getLanguages = GetLanguagesUseCase(repository),
            loadAndCacheLanguages = LoadAndCacheLanguagesUseCase(repository)
        )
    }

    @Provides
    @Singleton
    fun provideSetupUseCases(repository: SetupRepository): SetupUseCases {
        Log.d(TAG, "Hilt is calling: provideSetupUseCases")
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
        Log.d(TAG, "Hilt is calling: provideLeaguesUseCases")
        return LeaguesUseCases(
            getLeagues = GetLeaguesUseCase(repository),
            getLeagueTable = GetLeagueTableUseCase(repository),
            createLeague = CreateLeagueUseCase(repository)
        )
    }

    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
        return TokenManager(context) // Or however you initialize your TokenManager
    }

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            // Configure your JSON settings here
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
}


/**
 * This module contains all the @Binds functions.
 * These are abstract functions that tell Hilt which implementation to use for an interface.
 * Because it contains abstract methods, it MUST be an 'abstract class'.
 */
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



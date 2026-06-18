package cloud.scoreprof.app.di

import cloud.scoreprof.app.data.ScoreProfDatabase
import cloud.scoreprof.app.data.getDatabase
import cloud.scoreprof.app.data.getDatabaseBuilder
import org.koin.dsl.module

val databaseModule = module {
    single { getDatabase(getDatabaseBuilder()) }
    single { get<ScoreProfDatabase>().scoreProfDao() }
}

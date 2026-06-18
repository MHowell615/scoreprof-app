package cloud.scoreprof.app.di

import com.russhwolf.settings.Settings
import org.koin.dsl.module

val settingsModule = module {
    single<Settings> { Settings() }
}

package org.vitrivr.vitrivrapp.di

import dagger.Component
import org.vitrivr.vitrivrapp.data.helper.SharedPreferenceHelper
import org.vitrivr.vitrivrapp.features.query.QueryViewModel
import org.vitrivr.vitrivrapp.features.settings.SettingsViewModel
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {

    fun inject(sharedPreferenceHelper: SharedPreferenceHelper)
    fun inject(viewModel: QueryViewModel)
    fun inject(viewModel: SettingsViewModel)

}
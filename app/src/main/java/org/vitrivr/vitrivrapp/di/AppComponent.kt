package org.vitrivr.vitrivrapp.di

import dagger.Component
import org.vitrivr.vitrivrapp.data.helper.SharedPreferenceHelper
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {

    fun inject(sharedPreferenceHelper: SharedPreferenceHelper)

}
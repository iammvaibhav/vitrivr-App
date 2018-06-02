package org.vitrivr.vitrivrapp

import android.app.Application
import org.vitrivr.vitrivrapp.di.AppModule
import org.vitrivr.vitrivrapp.di.DaggerAppComponent

class App: Application() {

    companion object {
        lateinit var daggerAppComponent: DaggerAppComponent
    }

    override fun onCreate() {
        super.onCreate()
        daggerAppComponent = DaggerAppComponent.builder().appModule(AppModule(this)).build() as DaggerAppComponent
    }

}
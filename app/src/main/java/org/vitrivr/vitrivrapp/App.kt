package org.vitrivr.vitrivrapp

import android.app.Application
import org.vitrivr.vitrivrapp.di.DaggerAppComponent

class App: Application() {

    companion object {
        lateinit var daggerAppComponent: DaggerAppComponent
    }

    override fun onCreate() {
        super.onCreate()
        daggerAppComponent = DaggerAppComponent.create() as DaggerAppComponent
    }

}
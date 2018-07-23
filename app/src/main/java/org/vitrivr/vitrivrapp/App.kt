package org.vitrivr.vitrivrapp

import android.app.Application
import com.dmitrybrant.modelviewer.ModelViewerApplication
import net.gotev.uploadservice.UploadService
import net.gotev.uploadservice.okhttp.OkHttpStack
import okhttp3.OkHttpClient
import org.vitrivr.vitrivrapp.di.AppModule
import org.vitrivr.vitrivrapp.di.DaggerAppComponent
import javax.inject.Inject

/**
 * Custom Application class used to provide DaggerAppComponent and initialize services.
 */
class App: Application() {

    companion object {
        lateinit var daggerAppComponent: DaggerAppComponent
    }

    @Inject
    lateinit var okHttpClient: OkHttpClient

    override fun onCreate() {
        super.onCreate()

        /**
         * Configuring Dagger
         */
        daggerAppComponent = DaggerAppComponent.builder().appModule(AppModule(this)).build() as DaggerAppComponent
        daggerAppComponent.inject(this)

        /**
         * Configuring 3D Model Viewer
         */
        ModelViewerApplication.setResources(resources)

        /**
         * Configuring UploadService
         */
        UploadService.NAMESPACE = BuildConfig.APPLICATION_ID
        UploadService.HTTP_STACK = OkHttpStack(okHttpClient)
    }

}
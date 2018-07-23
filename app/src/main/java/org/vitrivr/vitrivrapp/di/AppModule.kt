package org.vitrivr.vitrivrapp.di

import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import org.vitrivr.vitrivrapp.data.repository.QueryRepository
import org.vitrivr.vitrivrapp.data.repository.QueryResultsRepository
import org.vitrivr.vitrivrapp.data.services.SettingsService
import org.vitrivr.vitrivrapp.features.results.PathUtils
import javax.inject.Singleton

@Module
class AppModule(val applicationContext: Context) {

    @Singleton
    @Provides
    fun provideGson() = Gson()

    @Singleton
    @Provides
    fun provideApplicationContext() = applicationContext

    @Singleton
    @Provides
    fun provideOkHttpClient() = OkHttpClient()

    @Singleton
    @Provides
    fun providePathUtils() = PathUtils()

    @Singleton
    @Provides
    fun provideSettingsSerivce() = SettingsService()

    @Singleton
    @Provides
    fun provideQueryResultsRepository() = QueryResultsRepository()

    @Singleton
    @Provides
    fun provideQueryRepository() = QueryRepository()

}
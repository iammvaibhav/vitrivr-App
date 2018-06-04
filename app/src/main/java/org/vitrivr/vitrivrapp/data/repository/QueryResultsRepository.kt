package org.vitrivr.vitrivrapp.data.repository

import android.arch.lifecycle.LiveData
import android.util.Log
import org.vitrivr.vitrivrapp.App
import org.vitrivr.vitrivrapp.data.model.results.QueryResultBaseModel
import org.vitrivr.vitrivrapp.data.services.QueryResultsService
import org.vitrivr.vitrivrapp.data.services.SettingsService
import javax.inject.Inject

class QueryResultsRepository {

    @Inject
    lateinit var settingsService: SettingsService
    @Inject
    lateinit var queryResultsService: QueryResultsService

    init {
        App.daggerAppComponent.inject(this)
    }

    fun getQueryResults(query: String, failure: (reason: String) -> Unit, closed: (code: Int) -> Unit): LiveData<QueryResultBaseModel> {
        val serverSettings = settingsService.getServerSettings()
        Log.e("serverSettings", serverSettings.value.toString())
        return queryResultsService.getQueryResults(query,
                "ws://${serverSettings.value?.address}:${serverSettings.value?.port}/api/v1",
                failure, closed)
    }
}
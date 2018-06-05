package org.vitrivr.vitrivrapp.data.repository

import io.reactivex.Observable
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

    fun getQueryResults(query: String): Observable<QueryResultBaseModel> {
        val serverSettings = settingsService.getServerSettings()
        return queryResultsService.getQueryResults(query,
                "ws://${serverSettings?.address}:${serverSettings?.port}/api/v1")
    }

    fun getDirectoryPath(): String {
        return "http://${settingsService.getServerSettings()?.address}:8081/data/image/"
    }
}
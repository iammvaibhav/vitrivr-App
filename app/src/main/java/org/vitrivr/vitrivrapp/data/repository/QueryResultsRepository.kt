package org.vitrivr.vitrivrapp.data.repository

import android.content.Context
import io.reactivex.Observable
import org.vitrivr.vitrivrapp.App
import org.vitrivr.vitrivrapp.data.helper.SharedPreferenceHelper
import org.vitrivr.vitrivrapp.data.model.results.QueryResultBaseModel
import org.vitrivr.vitrivrapp.data.model.results.QueryResultPresenterModel
import org.vitrivr.vitrivrapp.data.services.QueryResultsService
import org.vitrivr.vitrivrapp.data.services.SettingsService
import javax.inject.Inject

class QueryResultsRepository @Inject constructor(context: Context) {

    val QUERY_RESULTS_KEY = "QUERY_RESULTS_KEY"
    val CURRENT_PRESENTER_RESULTS = "CURRENT_PRESENTER_RESULTS"

    @Inject
    lateinit var settingsService: SettingsService
    @Inject
    lateinit var queryResultsService: QueryResultsService
    private val spHelper = SharedPreferenceHelper(context, QUERY_RESULTS_KEY)


    init {
        App.daggerAppComponent.inject(this)
    }

    fun getQueryResults(query: String): Observable<QueryResultBaseModel> {
        val serverSettings = settingsService.getServerSettings()
        return queryResultsService.getQueryResults(query,
                "ws://${serverSettings?.address}:${serverSettings?.port}/api/v1")
    }

    fun putCurrentPresenterResults(results: List<QueryResultPresenterModel>) {
        spHelper.putListObject(CURRENT_PRESENTER_RESULTS, results)
    }

    fun getCurrentPresenterResults(): List<QueryResultPresenterModel>? {
        return spHelper.getObjectList(CURRENT_PRESENTER_RESULTS)
    }


}
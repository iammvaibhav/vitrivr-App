package org.vitrivr.vitrivrapp.data.repository

import com.google.gson.reflect.TypeToken
import io.reactivex.Observable
import org.vitrivr.vitrivrapp.App
import org.vitrivr.vitrivrapp.data.helper.SharedPreferenceHelper
import org.vitrivr.vitrivrapp.data.model.results.QueryResultBaseModel
import org.vitrivr.vitrivrapp.data.model.results.QueryResultPresenterModel
import org.vitrivr.vitrivrapp.data.services.QueryResultsService
import org.vitrivr.vitrivrapp.data.services.SettingsService
import javax.inject.Inject

class QueryResultsRepository {

    val QUERY_RESULTS_KEY = "QUERY_RESULTS_KEY"
    val CURRENT_PRESENTER_RESULTS = "CURRENT_PRESENTER_RESULTS"

    @Inject
    lateinit var settingsService: SettingsService
    @Inject
    lateinit var queryResultsService: QueryResultsService
    private val spHelper = SharedPreferenceHelper(QUERY_RESULTS_KEY)

    init {
        App.daggerAppComponent.inject(this)
    }

    fun getQueryResults(query: String): Observable<QueryResultBaseModel>? {
        settingsService.getWebSocketEndpointURL()?.let {
            return queryResultsService.getQueryResults(query, it)
        }
        return null
    }

    fun putCurrentPresenterResults(results: List<QueryResultPresenterModel>) {
        spHelper.putListObject(CURRENT_PRESENTER_RESULTS, results)
    }

    fun getCurrentPresenterResults(): List<QueryResultPresenterModel>? {
        val typetoken = object : TypeToken<List<QueryResultPresenterModel>>() {}
        return spHelper.getObjectList(CURRENT_PRESENTER_RESULTS, typetoken)
    }
}
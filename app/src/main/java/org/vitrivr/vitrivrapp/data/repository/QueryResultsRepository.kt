package org.vitrivr.vitrivrapp.data.repository

import com.google.gson.reflect.TypeToken
import io.reactivex.Observable
import org.vitrivr.vitrivrapp.App
import org.vitrivr.vitrivrapp.data.helper.SharedPreferenceHelper
import org.vitrivr.vitrivrapp.data.model.enums.MediaType
import org.vitrivr.vitrivrapp.data.model.enums.ResultViewType
import org.vitrivr.vitrivrapp.data.model.results.QueryResultBaseModel
import org.vitrivr.vitrivrapp.data.model.results.QueryResultPresenterModel
import org.vitrivr.vitrivrapp.data.services.QueryResultsService
import org.vitrivr.vitrivrapp.data.services.SettingsService
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap

@Suppress("PrivatePropertyName")
class QueryResultsRepository {

    private val PREF_NAME_QUERY_RESULTS_KEY = "PREF_NAME_QUERY_RESULTS_KEY"

    private val QR_KEY_CURRENT_PRESENTER_RESULTS = "QR_KEY_CURRENT_PRESENTER_RESULTS"
    private val QR_KEY_CURRENT_RESULT_VIEW = "QR_KEY_CURRENT_RESULT_VIEW"
    private val QR_KEY_MEDIA_TYPE_CATEGORIES = "QR_KEY_MEDIA_TYPE_CATEGORIES"
    private val QR_KEY_MEDIA_TYPE_VISIBILITY = "QR_KEY_MEDIA_TYPE_VISIBILITY"
    private val QR_KEY_CATEGORY_WEIGHTS = "QR_KEY_CATEGORY_WEIGHTS"

    @Inject
    lateinit var settingsService: SettingsService

    @Inject
    lateinit var queryResultsService: QueryResultsService

    private val spHelper = SharedPreferenceHelper(PREF_NAME_QUERY_RESULTS_KEY)

    init {
        App.daggerAppComponent.inject(this)
    }

    /**
     * takes a query string and returns an observable to the results
     * @param query query string
     * @return Observable pushing QueryResultBaseModel objects if API settings are found. If not, returns null
     */
    fun getQueryResults(query: String): Observable<QueryResultBaseModel>? {
        settingsService.getWebSocketEndpointURL()?.let {
            return queryResultsService.getQueryResults(query, it)
        }
        return null
    }

    /**
     * put the current presenter list to persistent storage
     * @param results list to put
     */
    fun putCurrentPresenterResults(results: List<QueryResultPresenterModel>) {
        spHelper.putListObject(QR_KEY_CURRENT_PRESENTER_RESULTS, results)
    }

    /**
     * get the current presenter list from persistent storage
     * @return List of QueryResultPresenterModel
     */
    fun getCurrentPresenterResults(): List<QueryResultPresenterModel>? {
        return spHelper.getObjectList(QR_KEY_CURRENT_PRESENTER_RESULTS, object : TypeToken<List<QueryResultPresenterModel>>() {})
    }

    /**
     * put the current result view in the persistent storage
     * @param resultViewType ResultViewType to put
     */
    fun putCurrentResultView(resultViewType: ResultViewType) {
        spHelper.putString(QR_KEY_CURRENT_RESULT_VIEW, resultViewType.name)
    }

    /**
     * get the current result view from persistent storage
     * @return ResultViewType
     */
    fun getCurrentResultView(): ResultViewType {
        return ResultViewType.valueOf(spHelper.getString(QR_KEY_CURRENT_RESULT_VIEW) ?: "LARGE")
    }

    /**
     * put the categoryCount in the persistent storage
     * @param categoryCount to put
     */
    fun putMediaTypeCategories(categoryCount: HashMap<MediaType, HashSet<String>>) {
        spHelper.putObject(QR_KEY_MEDIA_TYPE_CATEGORIES, categoryCount)
    }

    /**
     * get the category count from persistent storage
     * @return HashMap<MediaType, HashSet<String>>
     */
    fun getMediaTypeCategories(): HashMap<MediaType, HashSet<String>> {
        return spHelper.getObject(QR_KEY_MEDIA_TYPE_CATEGORIES, object : TypeToken<HashMap<MediaType, HashSet<String>>>() {}.type)
                ?: HashMap()
    }

    /**
     * put media type visibility to the persistent storage
     * @param mediaTypeVisibility to put
     */
    fun putMediaTypeVisibility(mediaTypeVisibility: HashMap<MediaType, Boolean>) {
        spHelper.putObject(QR_KEY_MEDIA_TYPE_VISIBILITY, mediaTypeVisibility)
    }

    /**
     * get the media type visibility from the persistent storage
     * @return HashMap<MediaType, Boolean>
     */
    fun getMediaTypeVisibility(): HashMap<MediaType, Boolean> {
        return spHelper.getObject(QR_KEY_MEDIA_TYPE_VISIBILITY, object : TypeToken<HashMap<MediaType, Boolean>>() {}.type)
                ?: HashMap()
    }

    /**
     * put the category weights to persistent storage
     * @param categoryWeight to put
     */
    fun putCategoryWeights(categoryWeight: HashMap<String, Double>) {
        spHelper.putObject(QR_KEY_CATEGORY_WEIGHTS, categoryWeight)
    }

    /**
     * return the category weights from the persistent storage
     * @return HashMap<String, Double>
     */
    fun getCategoryWeights(): HashMap<String, Double> {
        return spHelper.getObject(QR_KEY_CATEGORY_WEIGHTS, object : TypeToken<HashMap<String, Double>>() {}.type)
                ?: HashMap()
    }

    /**
     * remove all the objects related to query results from the persistent storage
     */
    fun removeAllQueryResultsData() {
        spHelper.removeKey(QR_KEY_CURRENT_RESULT_VIEW)
        spHelper.removeKey(QR_KEY_CURRENT_RESULT_VIEW)
        spHelper.removeKey(QR_KEY_CATEGORY_WEIGHTS)
        spHelper.removeKey(QR_KEY_MEDIA_TYPE_VISIBILITY)
        spHelper.removeKey(QR_KEY_MEDIA_TYPE_CATEGORIES)
    }
}
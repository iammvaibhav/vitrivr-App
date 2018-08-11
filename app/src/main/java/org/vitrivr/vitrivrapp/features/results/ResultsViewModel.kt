package org.vitrivr.vitrivrapp.features.results

import android.annotation.SuppressLint
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.reactivex.functions.Action
import io.reactivex.functions.Consumer
import org.vitrivr.vitrivrapp.App
import org.vitrivr.vitrivrapp.data.gson.AnnotationExclusionStrategy
import org.vitrivr.vitrivrapp.data.model.enums.MediaType
import org.vitrivr.vitrivrapp.data.model.enums.MessageType
import org.vitrivr.vitrivrapp.data.model.enums.ResultViewType
import org.vitrivr.vitrivrapp.data.model.query.QueryModel
import org.vitrivr.vitrivrapp.data.model.results.*
import org.vitrivr.vitrivrapp.data.repository.QueryRepository
import org.vitrivr.vitrivrapp.data.repository.QueryResultsRepository
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@Suppress("NestedLambdaShadowedImplicitParameter")
/**
 * ViewModel associated with the ResultsActivity
 */
class ResultsViewModel : ViewModel() {

    @Inject
    lateinit var queryResultsRepository: QueryResultsRepository

    @Inject
    lateinit var queryRepository: QueryRepository

    @Inject
    lateinit var gson: Gson

    var isNewViewModel = true

    /**
     * This stores the media type and the categories associated with that media type present in the results
     */
    var categoryCount: HashMap<MediaType, HashSet<String>> = HashMap()

    /**
     * This stores the media type and its visibility which is changed via the query refinement drawer
     */
    var mediaTypeVisibility: HashMap<MediaType, Boolean> = HashMap()

    /**
     * This stores the category weight which is adjusted via the query refinement drawer
     */
    var categoryWeight: HashMap<String, Double> = HashMap()

    /**
     * These lists stores the query results and the liveResultPresenterList is the LiveData which
     * exposes this result list.
     */
    private val resultPresenterList = ArrayList<QueryResultPresenterModel>()
    private val sortedResultPresenterList = ArrayList<QueryResultPresenterModel>()
    private val liveResultPresenterList = MutableLiveData<List<QueryResultPresenterModel>>()

    /**
     * Auxiliary object used for preparing final list. Maps the object id to the index in resultPresenterList
     * where it is stored. Helps in finding if a segment's object is already inserted in the list
     */
    private val insertedObjects = HashMap<String, Int>()

    /**
     * partial results of a present category
     */
    private var queryResultSegmentModel: QueryResultSegmentModel? = null
    private var queryResultObjectModel: QueryResultObjectModel? = null
    private var queryResultSimilarityModel: QueryResultSimilarityModel? = null

    /**
     * current result view type
     */
    var currResultViewType = ResultViewType.LARGE

    init {
        App.daggerAppComponent.inject(this)
    }

    /**
     * This observer observes the query messages received by websocket and get those message as a
     * QueryResultBaseModel object. message type is read from that object after which it is casted to
     * the appropriate class.
     */
    private val queryResultObserver = Consumer<QueryResultBaseModel> {

        when (it.messageType) {
            MessageType.QR_START -> {

                /**
                 * clear all lists
                 */
                resultPresenterList.clear()
                insertedObjects.clear()
                categoryCount.clear()
                mediaTypeVisibility.clear()
                categoryWeight.clear()

                queryResultSegmentModel = null
                queryResultObjectModel = null
                queryResultSimilarityModel = null
            }

            MessageType.QR_SEGMENT -> {
                queryResultSegmentModel = it as QueryResultSegmentModel
            }

            MessageType.QR_OBJECT -> {
                queryResultObjectModel = it as QueryResultObjectModel
            }

            MessageType.QR_SIMILARITY -> {
                queryResultSimilarityModel = it as QueryResultSimilarityModel

                if (queryResultSegmentModel != null && queryResultObjectModel != null && queryResultSimilarityModel != null) {

                    /**
                     * get all the available media types
                     */
                    val availableMediaTypes = HashSet<MediaType>()
                    queryResultObjectModel!!.content.forEach {
                        availableMediaTypes.add(it.mediatype)
                        mediaTypeVisibility[it.mediatype] = true
                    }
                    categoryWeight[queryResultSimilarityModel!!.category] = 1.0
                    /** Initial category weight is 1.0 */

                    /**
                     * fill the categoryCount based on the available media types
                     */
                    availableMediaTypes.forEach {
                        if (categoryCount.containsKey(it)) {
                            categoryCount[it]!!.add(queryResultSimilarityModel!!.category)
                        } else {
                            val categories = HashSet<String>()
                            categories.add(queryResultSimilarityModel!!.category)
                            categoryCount[it] = categories
                        }
                    }

                    /**
                     * prepare a category item and add to presenter results
                     */
                    val categoryItem = QueryResultCategoryModel(queryResultSegmentModel!!, queryResultObjectModel!!, queryResultSimilarityModel!!)
                    addToPresenterResults(categoryItem)

                    /**
                     * sort the result list and post the value
                     */
                    sortedResultPresenterList.clear()
                    resultPresenterList.forEach { sortedResultPresenterList.add(it.copy()) }
                    /** deep copy required. */
                    sortedResultPresenterList.sortByDescending { it.segmentDetail.matchValue }
                    liveResultPresenterList.postValue(sortedResultPresenterList)


                    queryResultSegmentModel = null
                    queryResultObjectModel = null
                    queryResultSimilarityModel = null
                }
            }

            else -> { /* Ignore */
            }
        }
    }

    @SuppressLint("CheckResult")
            /**
             * run the query and return the results
             * @param query A well formed JSON string representing a valid cineast query message
             * @param failure block to call in case of any failure in query
             * @param closed black to call when query completes
             * @return LiveData with pushes a list of QueryResultPresenterModel, the query results if API settings are found
             * else return null if not API settings are found
             */
    fun getQueryResults(query: String, failure: (reason: String) -> Unit, closed: () -> Unit): LiveData<List<QueryResultPresenterModel>>? {
        val queryResult = queryResultsRepository.getQueryResults(query) ?: return null

        queryResult.subscribe(queryResultObserver,
                Consumer { failure(it.message ?: "Failure") },
                Action { closed() })

        return liveResultPresenterList
    }

    /**
     * @return LiveData object which receives the current query results
     */
    fun getCurrentResults() = liveResultPresenterList

    /**
     * Extract QueryResultPresenterModels from provided QueryResultCategoryModel and add them to the
     * resultPresenterList
     * @param categoryItem QueryResultCategoryModel for the category received to extract results from
     */
    private fun addToPresenterResults(categoryItem: QueryResultCategoryModel) {

        /**
         * current category
         */
        val category = categoryItem.queryResultSimilarityModel.category

        /**
         * Initialize the segmentMatchedValue which is a map mapping segment id to the matched value for
         * that particular category and create an object map mapping objectId to the ObjectModel for that object
         */
        val segmentMatchedValue = HashMap<String, Double>()
        categoryItem.queryResultSimilarityModel.content.forEach { segmentMatchedValue[it.key] = it.value }

        val objectMap = HashMap<String, ObjectModel>()
        categoryItem.queryResultObjectModel.content.forEach { objectMap[it.objectId] = it }


        for (segment in categoryItem.queryResultSegmentModel.content) {

            /**
             * Using dummy object while filling all segments info. It will be replaced by actual object
             * at the end.
             */
            val dummySegmentObject = SegmentDetails("", 0.0, 0.0, HashMap())

            /**
             * get this segment's object
             */
            val segmentObject = objectMap[segment.objectId]

            /**
             * get matched value for this segment for this particular category
             */
            val segmentMatchedCategoryValue = segmentMatchedValue[segment.segmentId]

            if (segmentObject != null && segmentMatchedCategoryValue != null) {

                /**
                 * The list doesn't already contains the object, we must add it
                 */
                if (!insertedObjects.containsKey(segment.objectId)) {

                    /**
                     * create a presenter item
                     */
                    val presenterItem = QueryResultPresenterModel(segmentObject.name, segmentObject.path,
                            segmentObject.mediatype, segment.objectId,
                            0, dummySegmentObject, ArrayList(), true)

                    /**
                     * create a new SegmentDetails object and add this to the presenter item's list of all segments
                     */
                    val segmentDetails = SegmentDetails(segment.segmentId, 0.0, segment.startabs, HashMap())
                    segmentDetails.categoriesWeights[category] = segmentMatchedCategoryValue
                    presenterItem.allSegments.add(segmentDetails)

                    /**
                     * add the presenter result to the resultPresenterList and update the insertedObjects map
                     */
                    resultPresenterList.add(presenterItem)
                    insertedObjects[segment.objectId] = resultPresenterList.size - 1
                } else {
                    /**
                     * The list already contains the object of this particular segment,
                     * check if this particular segment exists. If yes, then add the category details
                     * else add the new segment to this object
                     */

                    val presenterItem = resultPresenterList[insertedObjects[segment.objectId]!!]

                    /**
                     * this variable is initially false, presenterItem's all segments are checked
                     * and if this segment is found, then it is set to true
                     */
                    var isThisSegmentPresent = false

                    presenterItem.allSegments.forEach {
                        if (it.segmentId == segment.segmentId) {
                            isThisSegmentPresent = true

                            if (it.categoriesWeights.containsKey(category)) {
                                /**
                                 * If an entry of the same category already exists, then choose the
                                 * one which has higher category weight.
                                 */
                                if (it.categoriesWeights[category]!! < segmentMatchedCategoryValue) {
                                    it.categoriesWeights[category] = segmentMatchedCategoryValue
                                }
                            } else {
                                /**
                                 * add this category and matched value
                                 */
                                it.categoriesWeights[category] = segmentMatchedCategoryValue
                            }
                        }
                    }

                    /**
                     * If this segment was not already present, then create a new SegmentDetails object,
                     * add this category and matched value, and add the SegmentDetails object to the list of
                     * all segments
                     */
                    if (!isThisSegmentPresent) {
                        val segmentDetails = SegmentDetails(segment.segmentId, 0.0, segment.startabs, HashMap())
                        segmentDetails.categoriesWeights[category] = segmentMatchedCategoryValue
                        presenterItem.allSegments.add(segmentDetails)
                    }
                }
            }
        }

        /**
         * The matched value of all the SegmentDetails object added in this method is currently 0.0
         * Update them accordingly
         */
        processPresenterResults()
    }

    /**
     * Process the list of presenter results and updates the match value for all the segments
     * taking categories weight (set in the query refinement) into consideration.
     *
     * For a segment with multiple categories, final matched value is found by taking the average of all
     * the categories matches.
     */
    private fun processPresenterResults() {
        /**
         * keeps track of the highest matched value to be updated in the presenterObject
         */
        var highestMatchValue: Double

        /**
         * This is the SegmentDetails object of the highest matched value
         */
        var segmentDetailObject: SegmentDetails?

        for (presenterObject in resultPresenterList) {

            /**
             * Initialize
             * highestMatchValue to be -1.0 as all the matches would be >=0, so -1.0 can be considered least
             * and hence the comparisons below will work even if there is only one segment
             */
            highestMatchValue = -1.0
            segmentDetailObject = null


            presenterObject.allSegments.forEach {
                /**
                 * calculate the weightSum taking query refinements weight into account.
                 */
                var weightSum = 0.0
                it.categoriesWeights.forEach {
                    weightSum += (categoryWeight[it.key] ?: 1.0) * it.value
                }

                /**
                 * divide the weightSum by the total number of categories to calculate the average
                 * and set it as the final matchValue
                 */
                it.matchValue = weightSum / categoryCount[presenterObject.mediaType]!!.size

                /**
                 * If this matchValue is higher than the previous accounted, then update the highestMatchValue
                 * and set the segmentDetailObject
                 */
                if (it.matchValue > highestMatchValue) {
                    highestMatchValue = it.matchValue
                    segmentDetailObject = it
                }
            }

            /**
             * If everything goes well, we'll have a non null segmentDetailObject for sure so !!
             */
            presenterObject.segmentDetail = segmentDetailObject!!
            presenterObject.numberOfSegments = presenterObject.allSegments.size

            /**
             * update visibility according to query refinement
             */
            presenterObject.visibility = mediaTypeVisibility[presenterObject.mediaType] ?: false
        }
    }

    /**
     * After updating the mediaTypeVisibility & categoryWeight objects, apply the refinement and post the results
     */
    fun applyRefinements() {
        /**
         * recalculate all the matched values
         */
        processPresenterResults()

        /**
         * sort the results and post the value
         */
        sortedResultPresenterList.clear()
        resultPresenterList.forEach { sortedResultPresenterList.add(it.copy()) }
        /** deep copy required. */
        sortedResultPresenterList.sortByDescending { it.segmentDetail.matchValue }
        liveResultPresenterList.postValue(sortedResultPresenterList)
    }

    /**
     * Get the stored Similarity Query object in JSON String
     * @return String representing a valid JSON similarity query
     */
    fun queryToJson(): String {
        return GsonBuilder().setExclusionStrategies(AnnotationExclusionStrategy()).create()
                .toJson(queryRepository.getQueryObject(), object : TypeToken<QueryModel>() {}.type)
    }

    /**
     * save the current view model state
     */
    fun saveCurrentViewModelState() {
        getCurrentResults().value?.let {
            queryResultsRepository.putCurrentPresenterResults(it)
        }
        queryResultsRepository.putCategoryWeights(categoryWeight)
        queryResultsRepository.putCurrentResultView(currResultViewType)
        queryResultsRepository.putMediaTypeCategories(categoryCount)
        queryResultsRepository.putMediaTypeVisibility(mediaTypeVisibility)
    }

    /**
     * restore the current view model state
     */
    fun restoreCurrentViewModelState() {
        queryResultsRepository.getCurrentPresenterResults()?.let {
            liveResultPresenterList.value = it
            resultPresenterList.addAll(it)
        }

        categoryWeight = queryResultsRepository.getCategoryWeights()
        currResultViewType = queryResultsRepository.getCurrentResultView()
        categoryCount = queryResultsRepository.getMediaTypeCategories()
        mediaTypeVisibility = queryResultsRepository.getMediaTypeVisibility()
    }

    /**
     * remove the view model state if exists
     */
    fun removeViewModelState() {
        queryResultsRepository.removeAllQueryResultsData()
    }
}
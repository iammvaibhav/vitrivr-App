package org.vitrivr.vitrivrapp.features.results

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.reactivex.functions.Action
import io.reactivex.functions.Consumer
import org.vitrivr.vitrivrapp.App
import org.vitrivr.vitrivrapp.data.model.enums.MediaType
import org.vitrivr.vitrivrapp.data.model.enums.MessageType
import org.vitrivr.vitrivrapp.data.model.enums.ResultViewType
import org.vitrivr.vitrivrapp.data.model.results.*
import org.vitrivr.vitrivrapp.data.repository.QueryResultsRepository
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

class ResultsViewModel : ViewModel() {

    @Inject
    lateinit var queryResultsRepository: QueryResultsRepository
    private var categoryCount: HashMap<MediaType, HashSet<String>> = HashMap()

    private val resultPresenterList = ArrayList<QueryResultPresenterModel>()
    private val sortedResultPresenterList = ArrayList<QueryResultPresenterModel>()

    private val insertedObjects = HashMap<String, Int>()
    private val liveResultPresenterList = MutableLiveData<List<QueryResultPresenterModel>>()

    private var queryResultSegmentModel: QueryResultSegmentModel? = null
    private var queryResultObjectModel: QueryResultObjectModel? = null
    private var queryResultSimilarityModel: QueryResultSimilarityModel? = null

    var isNewViewModel = true
    var currResultViewType = ResultViewType.LARGE

    private val queryResultObserver = Consumer<QueryResultBaseModel> {

        when (it.messageType) {
            MessageType.QR_START -> {
                resultPresenterList.clear()
                insertedObjects.clear()
                categoryCount.clear()

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
                    val availableMediaTypes = HashSet<MediaType>()
                    queryResultObjectModel!!.content.forEach {
                        availableMediaTypes.add(it.mediatype)
                    }

                    availableMediaTypes.forEach {
                        if (categoryCount.containsKey(it)) {
                            categoryCount[it]!!.add(queryResultSimilarityModel!!.category)
                        } else {
                            val categories = HashSet<String>()
                            categories.add(queryResultSimilarityModel!!.category)
                            categoryCount[it] = categories
                        }
                    }

                    val categoryItem = QueryResultCategoryModel(queryResultSegmentModel!!, queryResultObjectModel!!, queryResultSimilarityModel!!)
                    addToPresenterResults(categoryItem)
                    sortedResultPresenterList.clear()
                    sortedResultPresenterList.addAll(resultPresenterList)
                    sortedResultPresenterList.sortByDescending { it.segmentDetail.matchValue }
                    liveResultPresenterList.postValue(sortedResultPresenterList)
                    queryResultSegmentModel = null
                    queryResultObjectModel = null
                    queryResultSimilarityModel = null
                }
            }

            MessageType.QR_END -> {
                //QueryEnd
            }
        }
    }

    init {
        App.daggerAppComponent.inject(this)
    }

    fun getQueryResults(query: String, failure: (reason: String) -> Unit, closed: () -> Unit): LiveData<List<QueryResultPresenterModel>> {
        val queryResult = queryResultsRepository.getQueryResults(query)
        queryResult.subscribe(queryResultObserver, Consumer {
            failure(it.message ?: "Failure")
        }, Action { closed() })
        return liveResultPresenterList
    }

    fun getCurrentResults() = liveResultPresenterList

    private fun addToPresenterResults(categoryItem: QueryResultCategoryModel) {
        val category = categoryItem.queryResultSimilarityModel.category
        val categoryWeight = HashMap<String, Double>()
        val objectMap = HashMap<String, ObjectModel>()

        for (i in categoryItem.queryResultSimilarityModel.content) {
            categoryWeight[i.key] = i.value
        }
        for (i in categoryItem.queryResultObjectModel.content) {
            objectMap[i.objectId] = i
        }

        for (segment in categoryItem.queryResultSegmentModel.content) {

            /**
             * Using dummy object while filling all segments info. It will be replaced by actual object
             * at the end.
             */
            val dummySegmentObject = SegmentDetails("", 0.0, HashMap())

            val segmentObject = objectMap[segment.objectId]
            val segmentWeight = categoryWeight[segment.segmentId]

            if (segmentObject != null && segmentWeight != null) {

                //The list doesn't already contains the object, we must add it
                if (!insertedObjects.containsKey(segment.objectId)) {

                    val presenterItem = QueryResultPresenterModel(segmentObject.name, segmentObject.path,
                            segmentObject.mediatype, segment.objectId,
                            0, dummySegmentObject, ArrayList())

                    val segmentDetails = SegmentDetails(segment.segmentId, 0.0, HashMap())
                    segmentDetails.categoriesWeights[category] = segmentWeight

                    presenterItem.allSegments.add(segmentDetails)
                    resultPresenterList.add(presenterItem)
                    insertedObjects[segment.objectId] = resultPresenterList.size - 1

                } else {
                    /**
                     * The list already contains the object of this particular segment,
                     * check if this particular segment exists. If yes, then add the category details
                     * else add the new segment to this object
                     */

                    val presenterItem = resultPresenterList[insertedObjects[segment.objectId]!!]
                    var isThisSegmentPresent = false

                    presenterItem.allSegments.forEach {
                        if (it.segmentId == segment.segmentId) {
                            isThisSegmentPresent = true
                            if (it.categoriesWeights.containsKey(category)) {
                                /**
                                 * If an entry of the same category already exists, then choose the
                                 * one which has higher category weight.
                                 */
                                if (it.categoriesWeights[category]!! < segmentWeight) {
                                    it.categoriesWeights[category] = segmentWeight
                                }
                            } else {
                                it.categoriesWeights[category] = segmentWeight
                            }
                        }
                    }

                    if (!isThisSegmentPresent) {
                        val segmentDetails = SegmentDetails(segment.segmentId, 0.0, HashMap())
                        segmentDetails.categoriesWeights[category] = segmentWeight
                        presenterItem.allSegments.add(segmentDetails)
                    }
                }
            }
        }

        processPresenterResults()
    }

    private fun processPresenterResults() {
        var highestMatchValue: Double
        var segmentDetailObject: SegmentDetails?
        for (presenterObject in resultPresenterList) {
            highestMatchValue = 0.0
            segmentDetailObject = null
            presenterObject.allSegments.forEach {
                it.matchValue = it.categoriesWeights.values.sum() / categoryCount[presenterObject.mediaType]!!.size
                if (it.matchValue > highestMatchValue) {
                    highestMatchValue = it.matchValue
                    segmentDetailObject = it
                }
            }
            presenterObject.segmentDetail = segmentDetailObject!!
            presenterObject.numberOfSegments = presenterObject.allSegments.size
        }
    }
}
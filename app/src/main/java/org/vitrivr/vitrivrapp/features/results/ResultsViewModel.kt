package org.vitrivr.vitrivrapp.features.results

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.util.Log
import org.vitrivr.vitrivrapp.App
import org.vitrivr.vitrivrapp.data.model.enums.MediaType
import org.vitrivr.vitrivrapp.data.model.enums.MessageType
import org.vitrivr.vitrivrapp.data.model.results.*
import org.vitrivr.vitrivrapp.data.repository.QueryResultsRepository
import javax.inject.Inject

class ResultsViewModel : ViewModel() {

    @Inject
    lateinit var queryResultsRepository: QueryResultsRepository
    var query: String = """
         {"messageType":"Q_SIM","containers":[{"terms":[{"data":"data:image/png;base64,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","categories":["globalcolor","localcolor"],"type":"IMAGE"}]},{"terms":[{"data":"data:image/png;base64,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","categories":["globalcolor","localcolor","quantized","edge"],"type":"IMAGE"}]}]}
        """
    var categoryCount: HashMap<MediaType, HashSet<String>> = HashMap()

    val resultPresenterList = ArrayList<QueryResultPresenterModel>()

    private val insertedObjects = HashMap<String, Int>()
    private val liveResultPresenterList = MutableLiveData<List<QueryResultPresenterModel>>()

    var queryResultCategories = ArrayList<QueryResultCategoryModel>()
    var queryResultSegmentModel: QueryResultSegmentModel? = null
    var queryResultObjectModel: QueryResultObjectModel? = null
    var queryResultSimilarityModel: QueryResultSimilarityModel? = null

    lateinit var removeObserverCallback: () -> Unit

    val queryResultObserver = Observer<QueryResultBaseModel> {
        when (it?.messageType) {
            MessageType.QR_START -> {
                queryResultCategories.clear()
                resultPresenterList.clear()
                insertedObjects.clear()
                categoryCount.clear()
                queryResultSegmentModel = null
                queryResultObjectModel = null
                queryResultSimilarityModel = null
            }

            MessageType.QR_SEGMENT -> {
                queryResultSegmentModel = it as QueryResultSegmentModel?
            }

            MessageType.QR_OBJECT -> {
                queryResultObjectModel = it as QueryResultObjectModel?
            }

            MessageType.QR_SIMILARITY -> {
                queryResultSimilarityModel = it as QueryResultSimilarityModel?
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

                    Log.e("categoryCount", categoryCount.toString())

                    val categoryItem = QueryResultCategoryModel(queryResultSegmentModel!!, queryResultObjectModel!!, queryResultSimilarityModel!!)
                    addToPresenterResults(categoryItem)
                    liveResultPresenterList.postValue(resultPresenterList)
                    queryResultSegmentModel = null
                    queryResultObjectModel = null
                    queryResultSimilarityModel = null
                }
            }

            MessageType.QR_END -> {
                removeObserverCallback()
            }
        }
    }

    init {
        App.daggerAppComponent.inject(this)
    }

    fun getQueryResults(failure: (reason: String) -> Unit, closed: (code: Int) -> Unit): LiveData<List<QueryResultPresenterModel>> {
        val queryResult = queryResultsRepository.getQueryResults(query, failure, closed)
        removeObserverCallback = {
            queryResult.removeObserver(queryResultObserver)
        }
        queryResult.observeForever(queryResultObserver)
        return liveResultPresenterList
    }

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
                                 * If an entry of the same category already exists, then choose from
                                 * that and the current one which has higher category weight.
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
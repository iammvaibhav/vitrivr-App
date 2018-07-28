package org.vitrivr.vitrivrapp.features.query

import android.arch.lifecycle.ViewModel
import android.net.Uri
import com.google.gson.Gson
import org.vitrivr.vitrivrapp.App
import org.vitrivr.vitrivrapp.data.model.enums.MessageType
import org.vitrivr.vitrivrapp.data.model.enums.QueryTermType
import org.vitrivr.vitrivrapp.data.model.query.LocationQueryDataModel
import org.vitrivr.vitrivrapp.data.model.query.QueryContainerModel
import org.vitrivr.vitrivrapp.data.model.query.QueryModel
import org.vitrivr.vitrivrapp.data.model.query.QueryTermModel
import org.vitrivr.vitrivrapp.data.repository.QueryRepository
import javax.inject.Inject

/**
 * ViewModel for QueryActivity
 */
class QueryViewModel : ViewModel() {

    @Inject
    lateinit var gson: Gson
    @Inject
    lateinit var queryRepository: QueryRepository

    /**
     * A similarity QueryModel which holds the query data
     */
    var query = QueryModel(MessageType.Q_SIM, ArrayList())

    /**
     * Whenever user clicks on a query term of a container, currContainerID gets updated
     * with the ID of that particular container
     */
    var currContainerID = 0L

    /**
     * Whenever user clicks on a query term of a container, currTermType gets updated
     * with the QueryTermType of that particular term
     */
    var currTermType = QueryTermType.IMAGE

    /**
     * Flag used by QueryActivity to find out if view model state is to be restored.
     * If it is new, and state is available then restore the state
     */
    var isNewViewModel = true

    init {
        App.daggerAppComponent.inject(this)
    }

    /**
     * Adds a QueryContainerModel to the query object and returns the containerID.
     * containerID is obtained by System.currentTimeMillis().
     * @return A Long containerID
     */
    fun addContainer(): Long {
        val containerID = System.currentTimeMillis()
        val newContainer = QueryContainerModel(containerID, "", ArrayList())
        query.containers.add(newContainer)
        return containerID
    }

    /**
     * removes a QueryContainerModel from the query object
     * @param containerID ID of the container to remove from query object
     */
    fun removeContainer(containerID: Long) {
        query.containers.removeAll { it.id == containerID }
    }

    /**
     * set the query description of a container with given containerID
     * @param containerID ID of the container to set description of
     * @param queryDescription description to be set
     */
    fun setQueryDescriptionOfContainer(containerID: Long, queryDescription: String) {
        getContainerWithID(containerID)?.description = queryDescription
    }

    /**
     * adds a query term of type QueryTermType to container with given containerID with default categories
     * @param containerID ID of the container to add type into
     * @param type QueryTermType of the query term to add into container
     */
    fun addQueryTermToContainer(containerID: Long, type: QueryTermType) {
        val categories = when (type) {
            QueryTermType.IMAGE -> arrayListOf("globalcolor", "localcolor")
            QueryTermType.AUDIO -> arrayListOf("audiofingerprint")
            QueryTermType.MODEL3D -> arrayListOf("sphericalharmonicslow")
            QueryTermType.MOTION -> arrayListOf("motion")
            QueryTermType.TEXT -> arrayListOf()
            QueryTermType.LOCATION -> arrayListOf("location")
        }
        getContainerWithID(containerID)?.terms?.add(QueryTermModel("", categories, type))
    }

    /**
     * removes a query term of type QueryTermType from the container with given containerID
     * @param containerID ID of the container to remove query term from
     * @param type QueryTermType of the query term to remove
     */
    fun removeQueryTermFromContainer(containerID: Long, type: QueryTermType) {
        getContainerWithID(containerID)?.terms?.removeAll { it.type == type }
    }

    /**
     * set the data of a query term. takes a base64 string of the data for all query terms except TEXT and LOCATION
     * @param containerID ID of the container to set data for
     * @param type QueryTermType to set data for
     * @param base64String data encoded in base64
     * @param dataType used in case of MODEL3D to distinguish between data type - model and image
     */
    fun setDataOfQueryTerm(containerID: Long, type: QueryTermType, base64String: String, dataType: Int = 0) {
        val term = getTermWithContainerID(containerID, type)

        val data = when (type) {
            QueryTermType.IMAGE -> "data:image/png;base64,$base64String"

            QueryTermType.AUDIO -> "data:audio/wav;base64,$base64String"

            QueryTermType.MODEL3D -> {
                if (dataType == 0)
                    "data:application/3d-json;base64,$base64String"
                else "data:image/png;base64,$base64String"
            }

            QueryTermType.MOTION -> "data:application/json;base64,$base64String"

            QueryTermType.TEXT -> base64String

            QueryTermType.LOCATION -> base64String
        }

        term?.data = data
    }

    /**
     * set the balance of a query term type. Applicable only for IMAGE, AUDIO and MODEL3D types
     * @param containerID ID of the container of which balance is to be set
     * @param type QueryTermType of the query term of which balance is to be set
     * @param progress balance progress (value depends on QueryTermType)
     */
    fun setBalance(containerID: Long, type: QueryTermType, progress: Int) {
        val term = getTermWithContainerID(containerID, type)
        term?.categories?.clear()

        when (type) {
            QueryTermType.IMAGE -> {
                when (progress) {
                    0 -> term?.categories?.addAll(listOf("globalcolor", "localcolor"))
                    1 -> term?.categories?.addAll(listOf("globalcolor", "localcolor", "quantized"))
                    2 -> term?.categories?.addAll(listOf("globalcolor", "localcolor", "quantized", "edge"))
                    3 -> term?.categories?.addAll(listOf("quantized", "localcolor", "localfeatures", "edge"))
                    4 -> term?.categories?.addAll(listOf("localcolor", "localfeatures", "edge"))
                }
            }
            QueryTermType.AUDIO -> {
                when (progress) {
                    0 -> term?.categories?.addAll(listOf("audiofingerprint"))
                    1 -> term?.categories?.addAll(listOf("audiofingerprint", "audiomatching"))
                    2 -> term?.categories?.addAll(listOf("audiomatching", "hpcpaverage"))
                    3 -> term?.categories?.addAll(listOf("audiomelody", "pitchsequence"))
                    4 -> term?.categories?.addAll(listOf("pitchsequence"))
                }
            }
            QueryTermType.MODEL3D -> {
                when (progress) {
                    0 -> term?.categories?.addAll(listOf("sphericalharmonicslow"))
                    1 -> term?.categories?.addAll(listOf("sphericalharmonicsdefault"))
                    2 -> term?.categories?.addAll(listOf("sphericalharmonicshigh", "lightfield"))
                }
            }
            else -> {
                /**
                 * Do nothing
                 */
            }
        }
    }

    /**
     * get the balance of a query term (Applicable only for IMAGE, AUDIO & MODEL3D types)
     * @param containerID ID of container to get balance from
     * @param type QueryTermType of the query term to get balance of
     * @return balance in Int
     */
    fun getBalance(containerID: Long, type: QueryTermType): Int {
        val categories = getTermWithContainerID(containerID, type)?.categories

        categories?.let {
            when (type) {
                QueryTermType.IMAGE -> {
                    return when (categories) {
                        listOf("globalcolor", "localcolor") -> 0
                        listOf("globalcolor", "localcolor", "quantized") -> 1
                        listOf("globalcolor", "localcolor", "quantized", "edge") -> 2
                        listOf("quantized", "localcolor", "localfeatures", "edge") -> 3
                        listOf("localcolor", "localfeatures", "edge") -> 4
                        else -> 0
                    }
                }
                QueryTermType.AUDIO -> {
                    return when (categories) {
                        listOf("audiofingerprint") -> 0
                        listOf("audiofingerprint", "audiomatching") -> 1
                        listOf("audiomatching", "hpcpaverage") -> 2
                        listOf("audiomelody", "pitchsequence") -> 3
                        listOf("pitchsequence") -> 4
                        else -> 0
                    }
                }
                QueryTermType.MODEL3D -> {
                    return when (categories) {
                        listOf("sphericalharmonicslow") -> 0
                        listOf("sphericalharmonicsdefault") -> 1
                        listOf("sphericalharmonicshigh", "lightfield") -> 2
                        else -> 0
                    }
                }
                else -> return 0
            }
        }
        return 0
    }

    /**
     * Adds a category to TEXT QueryTermType
     * @param containerID ID of the container to add category into
     * @param category category to be added
     */
    fun addTextQueryCategory(containerID: Long, category: String) {
        val categories = getTermWithContainerID(containerID, QueryTermType.TEXT)?.categories
        val categorySet = HashSet(categories)
        categorySet.add(category)
        categories?.clear()
        categories?.addAll(categorySet)
    }

    /**
     * removes a category from a TEXT QueryTermType
     * @param containerID ID of the container to remove category from
     * @param category category to be removed
     */
    fun removeTextQueryCategory(containerID: Long, category: String) {
        val categories = getTermWithContainerID(containerID, QueryTermType.TEXT)?.categories
        categories?.remove(category)
    }

    /**
     * returns the categories of a TEXT QueryTermType if exists
     * @param containerID ID of the container to get categories from
     * @returns categories of TEXT QueryTermType if exists
     */
    fun getTextQueryCategories(containerID: Long): ArrayList<String>? {
        return getTermWithContainerID(containerID, QueryTermType.TEXT)?.categories
    }

    /**
     * returns the text query data of a container
     * @param containerID ID of the container to get text query data from
     * @returns text query data if exist else returns null
     */
    fun getTextQueryData(containerID: Long): String? {
        return getTermWithContainerID(containerID, QueryTermType.TEXT)?.data
    }

    /**
     * returns the location query data of a container
     * @param containerId ID of the container to retrieve Location query data from
     * @return LocationQueryDataModel of the container. If it does not exist, a new LocationQueryDataModel
     * is returned with both Latitude and Longitude values set to zero
     */
    fun getLocationQueryData(containerId: Long): LocationQueryDataModel {
        val data = getTermWithContainerID(containerId, QueryTermType.LOCATION)?.data
        if (data == null || data == "")
            return LocationQueryDataModel(0.0, 0.0)
        return gson.fromJson<LocationQueryDataModel>(data, LocationQueryDataModel::class.java)
    }

    /**
     * saves the view model state to persistent storage
     */
    fun saveQueryViewModelState() {
        queryRepository.putQueryObject(query)
        queryRepository.putCurrTermType(currTermType)
        queryRepository.putCurrContainerID(currContainerID)
    }

    /**
     * restores the view model state from persistent storage
     */
    fun restoreQueryViewModelState() {
        queryRepository.getQueryObject()?.let { this.query = it }
        queryRepository.getCurrTermType()?.let { this.currTermType = it }
        queryRepository.getCurrContainerID()?.let { this.currContainerID = it }
    }

    /**
     * removes the view model state from persistent storage
     */
    fun removeQueryViewModelState() {
        queryRepository.removeQueryObject()
        queryRepository.removeCurrTermType()
        queryRepository.removeCurrContainerID()
    }

    /**
     * put the model Uri of a container with given containerID.
     * @param containerID ID of a container to put Uri into
     * @param uri Uri of a model to put
     */
    fun putModelUri(containerID: Long, uri: Uri?) {
        queryRepository.putModelUri(containerID, uri)
    }

    /**
     * get the model Uri of a particular container ID if exists
     * @param containerID ID of the container to get Uri from
     * @return Uri of the model if exist else null
     */
    fun getModelUri(containerID: Long): Uri? {
        return queryRepository.getModelUri(containerID)
    }

    /**
     * removes the motion data of container from persistent storage if exists
     * @param containerID ID of the container to remove motion data from
     */
    fun removeMotionData(containerID: Long) {
        queryRepository.removeMotionData(containerID)
    }

    /**
     * returns a QueryContainerModel of a particular containerID if exist
     * @param containerID ID of the container to retrieve
     * @return QueryContainerModel if container is found  with containerID else null
     */
    private fun getContainerWithID(containerID: Long): QueryContainerModel? {
        return query.containers.find { it.id == containerID }
    }

    /**
     * returns a QueryTermModel of a particular containerID if exist
     * @param containerID ID of the container to retrieve QueryTermType from
     * @param type QueryTermType to retrieve
     * @return QueryTermModel if containerID exists and type is found else null
     */
    private fun getTermWithContainerID(containerID: Long, type: QueryTermType): QueryTermModel? {
        return query.containers.find { it.id == containerID }?.terms?.find { it.type == type }
    }
}
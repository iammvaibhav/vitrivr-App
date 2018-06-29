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

class QueryViewModel : ViewModel() {


    @Inject
    lateinit var gson: Gson
    @Inject
    lateinit var queryRepository: QueryRepository

    var query = QueryModel(MessageType.Q_SIM, ArrayList())
    var currContainerID = 0L
    var currTermType = QueryTermType.IMAGE
    var isNewViewModel = true

    init {
        App.daggerAppComponent.inject(this)
    }

    fun addContainer(): Long {
        val containerId = System.currentTimeMillis()
        val newContainer = QueryContainerModel(containerId, "", ArrayList())
        query.containers.add(newContainer)
        return containerId
    }

    fun removeContainer(containerId: Long) {
        val iterator = query.containers.iterator()
        while (iterator.hasNext()) {
            val container = iterator.next()
            if (container.id == containerId) {
                iterator.remove()
                break
            }
        }
    }

    fun getContainerWithId(containerId: Long): QueryContainerModel? {
        for (container in query.containers) {
            if (container.id == containerId)
                return container
        }
        return null
    }

    fun getTermInContainer(type: QueryTermType, container: QueryContainerModel?): QueryTermModel? {
        if (container == null)
            return null
        for (term in container.terms) {
            if (term.type == type)
                return term
        }
        return null
    }

    fun addQueryTermToContainer(containerId: Long, type: QueryTermType) {
        val categories = when (type) {
            QueryTermType.IMAGE -> arrayListOf("globalcolor", "localcolor")
            QueryTermType.AUDIO -> arrayListOf("audiofingerprint")
            QueryTermType.MODEL3D -> arrayListOf("sphericalharmonicslow")
            QueryTermType.MOTION -> arrayListOf("motion")
            QueryTermType.TEXT -> arrayListOf()
            QueryTermType.LOCATION -> arrayListOf("location")
        }
        getContainerWithId(containerId)?.terms?.add(QueryTermModel("", categories, type))
    }

    fun removeQueryTermFromContainer(containerId: Long, type: QueryTermType) {
        val container = getContainerWithId(containerId)
        if (container == null)
            return
        val termIterator = container.terms.iterator()
        while (termIterator.hasNext()) {
            val term = termIterator.next()
            if (term.type == type) {
                termIterator.remove()
                break
            }
        }
    }

    fun setQueryDescriptionOfContainer(containerId: Long, queryDescription: String) {
        val container = getContainerWithId(containerId)
        container?.description = queryDescription
    }

    fun setDataOfQueryTerm(containerId: Long, type: QueryTermType, base64String: String, dataType: Int = 0) {
        val term = getTermInContainer(type, getContainerWithId(containerId))
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

    fun setBalance(containerId: Long, type: QueryTermType, progress: Int) {
        val term = getTermInContainer(type, getContainerWithId(containerId))
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
        }
    }

    fun getBalance(containerId: Long, type: QueryTermType): Int {
        val categories = getTermInContainer(type, getContainerWithId(containerId))?.categories
        categories?.let {
            when (type) {
                QueryTermType.IMAGE -> {
                    when (categories) {
                        listOf("globalcolor", "localcolor") -> return 0
                        listOf("globalcolor", "localcolor", "quantized") -> return 1
                        listOf("globalcolor", "localcolor", "quantized", "edge") -> return 2
                        listOf("quantized", "localcolor", "localfeatures", "edge") -> return 3
                        listOf("localcolor", "localfeatures", "edge") -> return 4
                        else -> return 0
                    }
                }
                QueryTermType.AUDIO -> {
                    when (categories) {
                        listOf("audiofingerprint") -> return 0
                        listOf("audiofingerprint", "audiomatching") -> return 1
                        listOf("audiomatching", "hpcpaverage") -> return 2
                        listOf("audiomelody", "pitchsequence") -> return 3
                        listOf("pitchsequence") -> return 4
                        else -> return 0
                    }
                }
                QueryTermType.MODEL3D -> {
                    when (categories) {
                        listOf("sphericalharmonicslow") -> return 0
                        listOf("sphericalharmonicsdefault") -> return 1
                        listOf("sphericalharmonicshigh", "lightfield") -> return 2
                        else -> return 0
                    }
                }
                else -> return 0
            }
        }
        return 0
    }

    fun getTextQueryCategories(containerId: Long) = getTermInContainer(QueryTermType.TEXT, getContainerWithId(containerId))?.categories

    fun addTextQueryCategory(containerId: Long, category: String) {
        val categories = getTermInContainer(QueryTermType.TEXT, getContainerWithId(containerId))?.categories
        val categorySet = HashSet(categories)
        categorySet.add(category)
        categories?.clear()
        categories?.addAll(categorySet)
    }

    fun removeTextQueryCategory(containerId: Long, category: String) {
        val categories = getTermInContainer(QueryTermType.TEXT, getContainerWithId(containerId))?.categories
        categories?.remove(category)
    }

    fun getTextQueryData(containerId: Long) = getTermInContainer(QueryTermType.TEXT, getContainerWithId(containerId))?.data

    fun getLocationQueryData(containerId: Long): LocationQueryDataModel {
        val data = getTermInContainer(QueryTermType.LOCATION, getContainerWithId(containerId))?.data
        if (data == null || data == "")
            return LocationQueryDataModel(0.0, 0.0)
        return gson.fromJson<LocationQueryDataModel>(data, LocationQueryDataModel::class.java)
    }

    fun saveQueryObject() {
        queryRepository.putQueryObject(query)
    }

    fun restoreQueryObject() {
        queryRepository.getQueryObject()?.let {
            this.query = it
        }
    }

    fun putModelUri(uri: Uri?, containerId: Long) {
        queryRepository.putModelUri(uri, containerId)
    }

    fun getModelUri(containerId: Long) = queryRepository.getModelUri(containerId)

    fun removeMotionData(containerId: Long) {
        queryRepository.removeMotionData(containerId)
    }
}
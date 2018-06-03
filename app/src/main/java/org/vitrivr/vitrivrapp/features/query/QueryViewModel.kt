package org.vitrivr.vitrivrapp.features.query

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.vitrivr.vitrivrapp.App
import org.vitrivr.vitrivrapp.data.model.*
import org.vitrivr.vitrivrapp.data.repository.QueryResultsRepository
import org.vitrivr.vitrivrapp.features.query.QueryToggles.QueryTerm
import javax.inject.Inject

class QueryViewModel : ViewModel() {

    @Inject
    lateinit var gson: Gson
    @Inject
    lateinit var queryResultsRepository: QueryResultsRepository

    var query = QueryModel(MessageType.Q_SIM, ArrayList())
    var currContainerID = 0L
    var currTermType = QueryTerm.IMAGE
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

    fun getTermInContainer(type: QueryTerm, container: QueryContainerModel?): QueryTermModel? {
        if (container == null)
            return null
        for (term in container.terms) {
            if (term.type == type)
                return term
        }
        return null
    }

    fun addQueryTermToContainer(containerId: Long, type: QueryTerm) {
        val categories = when (type) {
            QueryTerm.IMAGE -> arrayListOf("globalcolor", "localcolor")
            QueryTerm.AUDIO -> arrayListOf("audiofingerprint")
            QueryTerm.MODEL3D -> arrayListOf("sphericalharmonicslow")
            QueryTerm.MOTION -> arrayListOf("motion")
            QueryTerm.TEXT -> arrayListOf()
            QueryTerm.LOCATION -> TODO("Location is not implemented yet")
        }
        getContainerWithId(containerId)?.terms?.add(QueryTermModel("", categories, type))
    }

    fun removeQueryTermFromContainer(containerId: Long, type: QueryTerm) {
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

    fun setDataOfQueryTerm(containerId: Long, type: QueryTerm, base64String: String) {
        val term = getTermInContainer(type, getContainerWithId(containerId))
        val data = when (type) {
            QueryTerm.IMAGE -> "data:image/png;base64,$base64String"
            QueryTerm.AUDIO -> "data:audio/wav;base64,$base64String"
            QueryTerm.MODEL3D -> "data:application/3d-json;base64,$base64String"
            QueryTerm.MOTION -> "data:application/json;base64,$base64String"
            QueryTerm.TEXT -> base64String
            QueryTerm.LOCATION -> TODO("Location is not implemented yet")
        }
        term?.data = data
    }

    fun setBalance(containerId: Long, type: QueryTerm, progress: Int) {
        val term = getTermInContainer(type, getContainerWithId(containerId))
        term?.categories?.clear()

        when (type) {
            QueryTerm.IMAGE -> {
                when (progress) {
                    0 -> term?.categories?.addAll(listOf("globalcolor", "localcolor"))
                    1 -> term?.categories?.addAll(listOf("globalcolor", "localcolor", "quantized"))
                    2 -> term?.categories?.addAll(listOf("globalcolor", "localcolor", "quantized", "edge"))
                    3 -> term?.categories?.addAll(listOf("quantized", "localcolor", "localfeatures", "edge"))
                    4 -> term?.categories?.addAll(listOf("localcolor", "localfeatures", "edge"))
                }
            }
            QueryTerm.AUDIO -> {
                when (progress) {
                    0 -> term?.categories?.addAll(listOf("audiofingerprint"))
                    1 -> term?.categories?.addAll(listOf("audiofingerprint", "audiomatching"))
                    2 -> term?.categories?.addAll(listOf("audiomatching", "hpcpaverage"))
                    3 -> term?.categories?.addAll(listOf("audiomelody", "pitchsequence"))
                    4 -> term?.categories?.addAll(listOf("pitchsequence"))
                }
            }
            QueryTerm.MODEL3D -> {
                when (progress) {
                    0 -> term?.categories?.addAll(listOf("sphericalharmonicslow"))
                    1 -> term?.categories?.addAll(listOf("sphericalharmonicsdefault"))
                    2 -> term?.categories?.addAll(listOf("sphericalharmonicshigh", "lightfield"))
                }
            }
        }
    }

    fun getBalance(containerId: Long, type: QueryTerm): Int {
        val categories = getTermInContainer(type, getContainerWithId(containerId))?.categories
        categories?.let {
            when (type) {
                QueryTerm.IMAGE -> {
                    when (categories) {
                        listOf("globalcolor", "localcolor") -> return 0
                        listOf("globalcolor", "localcolor", "quantized") -> return 1
                        listOf("globalcolor", "localcolor", "quantized", "edge") -> return 2
                        listOf("quantized", "localcolor", "localfeatures", "edge") -> return 3
                        listOf("localcolor", "localfeatures", "edge") -> return 4
                        else -> return 0
                    }
                }
                QueryTerm.AUDIO -> {
                    when (categories) {
                        listOf("audiofingerprint") -> return 0
                        listOf("audiofingerprint", "audiomatching") -> return 1
                        listOf("audiomatching", "hpcpaverage") -> return 2
                        listOf("audiomelody", "pitchsequence") -> return 3
                        listOf("pitchsequence") -> return 4
                        else -> return 0
                    }
                }
                QueryTerm.MODEL3D -> {
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

    fun queryToJson(): String {
        return gson.toJson(query, object : TypeToken<QueryModel>() {}.type)
    }

    fun search(failure: (reason: String) -> Unit, closed: (code: Int) -> Unit): LiveData<QueryResultBaseModel> {
        return queryResultsRepository.getQueryResults(queryToJson(), failure, closed)
    }
}
package org.vitrivr.vitrivrapp.data.services

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.google.gson.Gson
import okhttp3.*
import org.vitrivr.vitrivrapp.data.model.*
import javax.inject.Inject

class QueryResultsService @Inject constructor(val okHttpClient: OkHttpClient, val gson: Gson) {

    fun getQueryResults(query: String, url: String, failure: (reason: String) -> Unit, closed: (code: Int) -> Unit): LiveData<QueryResultBaseModel> {
        val request = Request.Builder()
                .url(url)
                .build()
        val queryListener = QueryResultsListener(query, failure, closed, gson)
        val webSocket = okHttpClient.newWebSocket(request, queryListener)
        return queryListener.getQueryResults()
    }

    class QueryResultsListener(val query: String, private val failure: (reason: String) -> Unit, private val closed: (code: Int) -> Unit, val gson: Gson) : WebSocketListener() {

        var queryResults: MutableLiveData<QueryResultBaseModel> = MutableLiveData()

        companion object {
            val CLOSE_CODE_NORMAL = 1000
        }

        fun getQueryResults(): LiveData<QueryResultBaseModel> {
            return queryResults
        }

        override fun onOpen(webSocket: WebSocket?, response: Response?) {
            webSocket?.send(query)
            webSocket?.close(CLOSE_CODE_NORMAL, "Query Completed")
        }

        override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
            super.onFailure(webSocket, t, response)
            failure(t?.message ?: "Failure")
        }

        override fun onMessage(webSocket: WebSocket?, text: String?) {
            super.onMessage(webSocket, text)

            if (text == null)
                failure("Null Message Received")

            text?.let {
                val baseQueryHelper = gson.fromJson<QueryResultBaseHelperModel>(it, QueryResultBaseHelperModel::class.java)
                val baseQuery = object : QueryResultBaseModel() {
                    override val messageType: MessageType
                        get() = baseQueryHelper.messageType
                }

                queryResults.postValue(when (baseQuery.messageType) {
                    MessageType.QR_START -> gson.fromJson(it, QueryResultStartModel::class.java)
                    MessageType.QR_END -> gson.fromJson(it, QueryResultEndModel::class.java)
                    MessageType.QR_SEGMENT -> gson.fromJson(it, QueryResultSegmentModel::class.java)
                    MessageType.QR_OBJECT -> gson.fromJson(it, QueryResultObjectModel::class.java)
                    MessageType.QR_SIMILARITY -> gson.fromJson(it, QueryResultSimilarityModel::class.java)
                    else -> baseQuery
                })
            }
        }

        override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
            super.onClosed(webSocket, code, reason)
            closed(code)
        }
    }
}
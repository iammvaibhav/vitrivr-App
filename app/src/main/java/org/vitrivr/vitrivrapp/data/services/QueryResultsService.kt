package org.vitrivr.vitrivrapp.data.services

import android.util.Log
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.ReplaySubject
import okhttp3.*
import org.vitrivr.vitrivrapp.data.model.enums.MessageType
import org.vitrivr.vitrivrapp.data.model.results.*
import javax.inject.Inject

class QueryResultsService @Inject constructor(val okHttpClient: OkHttpClient, val gson: Gson) {

    fun getQueryResults(query: String, url: String): Observable<QueryResultBaseModel> {
        val request = Request.Builder()
                .url(url)
                .build()
        val queryListener = QueryResultsListener(query, gson)
        val webSocket = okHttpClient.newWebSocket(request, queryListener)
        return queryListener.getQueryResults()
    }

    class QueryResultsListener(val query: String, val gson: Gson) : WebSocketListener() {

        private var queryResults: ReplaySubject<QueryResultBaseModel> = ReplaySubject.create()

        companion object {
            const val CLOSE_CODE_NORMAL = 1000
        }

        fun getQueryResults(): Observable<QueryResultBaseModel> {
            return queryResults.observeOn(AndroidSchedulers.mainThread())
        }

        override fun onOpen(webSocket: WebSocket?, response: Response?) {
            webSocket?.send(query)
            webSocket?.close(CLOSE_CODE_NORMAL, "Query Completed")
        }

        override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
            super.onFailure(webSocket, t, response)
            queryResults.onError(t ?: Throwable("Failure"))
        }

        override fun onMessage(webSocket: WebSocket?, text: String?) {
            super.onMessage(webSocket, text)
            Log.e("message", text)
            text?.let {
                val baseQueryHelper = gson.fromJson<QueryResultBaseHelperModel>(it, QueryResultBaseHelperModel::class.java)
                val baseQuery = object : QueryResultBaseModel() {
                    override val messageType: MessageType
                        get() = baseQueryHelper.messageType
                }

                queryResults.onNext(when (baseQuery.messageType) {
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
            queryResults.onComplete()
        }
    }
}
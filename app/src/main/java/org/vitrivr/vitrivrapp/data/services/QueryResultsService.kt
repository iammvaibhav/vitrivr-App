package org.vitrivr.vitrivrapp.data.services

import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.ReplaySubject
import okhttp3.*
import org.vitrivr.vitrivrapp.data.model.enums.MessageType
import org.vitrivr.vitrivrapp.data.model.results.*
import javax.inject.Inject

@Suppress("MemberVisibilityCanBePrivate")
/**
 * QueryResultsService class create a websocket connection for the queries.
 */
class QueryResultsService @Inject constructor(val okHttpClient: OkHttpClient, val gson: Gson) {

    /**
     * returns the query result
     * @param query A valid JSON string representing a valid query
     * @param url WebSocket url to connect to
     * @return Observable object which emits QueryResultBaseModel which can be cast to appropriate classes
     * based on the message type
     */
    fun getQueryResults(query: String, url: String): Observable<QueryResultBaseModel> {
        val request = Request.Builder()
                .url(url)
                .build()
        val queryListener = QueryResultsListener(query, gson)
        okHttpClient.newWebSocket(request, queryListener)
        return queryListener.getQueryResults()
    }

    /**
     * A WebSocket listener
     */
    class QueryResultsListener(val query: String, val gson: Gson) : WebSocketListener() {

        private var queryResults: ReplaySubject<QueryResultBaseModel> = ReplaySubject.create()

        companion object {
            const val CLOSE_CODE_NORMAL = 1000
        }

        /**
         * @return An Observable emitting QueryResultBaseModel objects
         */
        fun getQueryResults(): Observable<QueryResultBaseModel> {
            return queryResults.observeOn(AndroidSchedulers.mainThread())
        }

        override fun onOpen(webSocket: WebSocket?, response: Response?) {
            /**
             * send the query when the websocket gets opened
             */
            webSocket?.send(query)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            /**
             * notify on error
             */
            queryResults.onError(t)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)


            val baseQueryHelper = gson.fromJson<QueryResultBaseHelperModel>(text, QueryResultBaseHelperModel::class.java)
            val baseQuery = object : QueryResultBaseModel() {
                override val messageType: MessageType
                    get() = baseQueryHelper.messageType
            }

            /**
             * close the websocket when QR_END messageType message is received
             */
            if (baseQuery.messageType == MessageType.QR_END)
                webSocket.close(CLOSE_CODE_NORMAL, "Query Completed")

            /**
             * deserialize objects from the received JSON based on the message type
             */
            queryResults.onNext(when (baseQuery.messageType) {
                MessageType.QR_START -> gson.fromJson(text, QueryResultStartModel::class.java)
                MessageType.QR_END -> gson.fromJson(text, QueryResultEndModel::class.java)
                MessageType.QR_SEGMENT -> gson.fromJson(text, QueryResultSegmentModel::class.java)
                MessageType.QR_OBJECT -> gson.fromJson(text, QueryResultObjectModel::class.java)
                MessageType.QR_SIMILARITY -> gson.fromJson(text, QueryResultSimilarityModel::class.java)
                else -> baseQuery
            })
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            /**
             * notify on closed
             */
            queryResults.onComplete()
        }
    }
}
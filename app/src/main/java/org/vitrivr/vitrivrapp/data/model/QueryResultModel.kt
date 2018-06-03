package org.vitrivr.vitrivrapp.data.model

import org.vitrivr.vitrivrapp.features.query.QueryToggles

abstract class QueryResultBaseModel {
    abstract val messageType: MessageType
}

data class QueryResultBaseHelperModel(val messageType: MessageType)

data class QueryResultStartModel(val queryId: String, override val messageType: MessageType) : QueryResultBaseModel()
data class QueryResultEndModel(val queryId: String, override val messageType: MessageType) : QueryResultBaseModel()

data class SegmentModel(val segmentId: String, val objectId: String,
                        val start: Int, val end: Int, val startabs: Int, val endabs: Int,
                        val count: Int, val sequenceNumber: Int)

data class QueryResultSegmentModel(val content: ArrayList<SegmentModel>, val queryId: String,
                                   override val messageType: MessageType) : QueryResultBaseModel()

data class ObjectModel(val objectId: String, val name: String, val path: String, val mediaType: QueryToggles.QueryTerm)

data class QueryResultObjectModel(val content: ArrayList<ObjectModel>, val queryId: String, override val messageType: MessageType) : QueryResultBaseModel()

data class SimilarityModel(val key: String, val value: Double)

data class QueryResultSimilarityModel(val content: ArrayList<SimilarityModel>, val queryId: String, val category: String, override val messageType: MessageType) : QueryResultBaseModel()

enum class ServerStatus {
    OK, ERROR, DISCONNECTED
}

data class QueryServerStatusModel(override val messageType: MessageType, val status: ServerStatus) : QueryResultBaseModel()



package org.vitrivr.vitrivrapp.data.model.results

import android.os.Parcel
import android.os.Parcelable
import org.vitrivr.vitrivrapp.data.model.enums.MediaType
import org.vitrivr.vitrivrapp.data.model.enums.MessageType

abstract class QueryResultBaseModel {
    abstract val messageType: MessageType
}

data class QueryResultBaseHelperModel(val messageType: MessageType)

data class QueryResultStartModel(val queryId: String,
                                 override val messageType: MessageType) : QueryResultBaseModel()

data class QueryResultEndModel(val queryId: String,
                               override val messageType: MessageType) : QueryResultBaseModel()

data class SegmentModel(val segmentId: String,
                        val objectId: String,
                        val start: Int,
                        val end: Int,
                        val startabs: Double,
                        val endabs: Double,
                        val count: Int,
                        val sequenceNumber: Int)

data class QueryResultSegmentModel(val content: ArrayList<SegmentModel>,
                                   val queryId: String,
                                   override val messageType: MessageType) : QueryResultBaseModel()

data class ObjectModel(val objectId: String,
                       val name: String,
                       val path: String,
                       val mediatype: MediaType)

data class QueryResultObjectModel(val content: ArrayList<ObjectModel>,
                                  val queryId: String,
                                  override val messageType: MessageType) : QueryResultBaseModel()

data class SimilarityModel(val key: String,
                           val value: Double)

data class QueryResultSimilarityModel(val content: ArrayList<SimilarityModel>,
                                      val queryId: String,
                                      val category: String,
                                      override val messageType: MessageType) : QueryResultBaseModel()

enum class ServerStatus {
    OK, ERROR, DISCONNECTED
}

data class QueryServerStatusModel(override val messageType: MessageType, val status: ServerStatus) : QueryResultBaseModel()

data class SegmentDetails(val segmentId: String,
                          var matchValue: Double,
                          val startAbs: Double,
                          val categoriesWeights: HashMap<String, Double>) : Parcelable {
    constructor(source: Parcel) : this(
            source.readString(),
            source.readDouble(),
            source.readDouble(),
            source.readSerializable() as HashMap<String, Double>
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(segmentId)
        writeDouble(matchValue)
        writeDouble(startAbs)
        writeSerializable(categoriesWeights)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<SegmentDetails> = object : Parcelable.Creator<SegmentDetails> {
            override fun createFromParcel(source: Parcel): SegmentDetails = SegmentDetails(source)
            override fun newArray(size: Int): Array<SegmentDetails?> = arrayOfNulls(size)
        }
    }

    fun copy(): SegmentDetails {
        val categoriesWeights = HashMap<String, Double>()
        for ((i, j) in this.categoriesWeights) {
            categoriesWeights[i] = j
        }
        return SegmentDetails(this.segmentId, this.matchValue, this.startAbs, categoriesWeights)
    }
}

data class QueryResultPresenterModel(val fileName: String,
                                     val filePath: String,
                                     val mediaType: MediaType,
                                     val objectId: String,
                                     var numberOfSegments: Int,
                                     var segmentDetail: SegmentDetails,
                                     val allSegments: ArrayList<SegmentDetails>,
                                     var visibility: Boolean) : Parcelable {
    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            MediaType.values()[source.readInt()],
            source.readString(),
            source.readInt(),
            source.readParcelable<SegmentDetails>(SegmentDetails::class.java.classLoader),
            source.createTypedArrayList(SegmentDetails.CREATOR),
            1 == source.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(fileName)
        writeString(filePath)
        writeInt(mediaType.ordinal)
        writeString(objectId)
        writeInt(numberOfSegments)
        writeParcelable(segmentDetail, 0)
        writeTypedList(allSegments)
        writeInt((if (visibility) 1 else 0))
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<QueryResultPresenterModel> = object : Parcelable.Creator<QueryResultPresenterModel> {
            override fun createFromParcel(source: Parcel): QueryResultPresenterModel = QueryResultPresenterModel(source)
            override fun newArray(size: Int): Array<QueryResultPresenterModel?> = arrayOfNulls(size)
        }
    }

    fun copy(): QueryResultPresenterModel {
        val allSegments = ArrayList<SegmentDetails>()
        this.allSegments.forEach { allSegments.add(it.copy()) }
        return QueryResultPresenterModel(this.fileName, this.filePath, this.mediaType, this.objectId,
                this.numberOfSegments, this.segmentDetail.copy(), allSegments, this.visibility)
    }
}

data class QueryResultCategoryModel(val queryResultSegmentModel: QueryResultSegmentModel,
                                    val queryResultObjectModel: QueryResultObjectModel,
                                    val queryResultSimilarityModel: QueryResultSimilarityModel)
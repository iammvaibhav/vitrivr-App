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
                        val startabs: Int,
                        val endabs: Int,
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
                          val categoriesWeights: HashMap<String, Double>) : Parcelable {
    constructor(source: Parcel) : this(
            source.readString(),
            source.readDouble(),
            source.readSerializable() as HashMap<String, Double>
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(segmentId)
        writeDouble(matchValue)
        writeSerializable(categoriesWeights)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<SegmentDetails> = object : Parcelable.Creator<SegmentDetails> {
            override fun createFromParcel(source: Parcel): SegmentDetails = SegmentDetails(source)
            override fun newArray(size: Int): Array<SegmentDetails?> = arrayOfNulls(size)
        }
    }
}

data class QueryResultPresenterModel(val fileName: String,
                                     val filePath: String,
                                     val mediaType: MediaType,
                                     val objectId: String,
                                     var numberOfSegments: Int,
                                     var segmentDetail: SegmentDetails,
                                     val allSegments: ArrayList<SegmentDetails>) : Parcelable {
    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            MediaType.values()[source.readInt()],
            source.readString(),
            source.readInt(),
            source.readParcelable<SegmentDetails>(SegmentDetails::class.java.classLoader),
            source.createTypedArrayList(SegmentDetails.CREATOR)
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
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<QueryResultPresenterModel> = object : Parcelable.Creator<QueryResultPresenterModel> {
            override fun createFromParcel(source: Parcel): QueryResultPresenterModel = QueryResultPresenterModel(source)
            override fun newArray(size: Int): Array<QueryResultPresenterModel?> = arrayOfNulls(size)
        }
    }
}

data class QueryResultCategoryModel(val queryResultSegmentModel: QueryResultSegmentModel,
                                    val queryResultObjectModel: QueryResultObjectModel,
                                    val queryResultSimilarityModel: QueryResultSimilarityModel)
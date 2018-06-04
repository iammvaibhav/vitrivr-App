package org.vitrivr.vitrivrapp.data.model.query

import android.os.Parcel
import android.os.Parcelable
import org.vitrivr.vitrivrapp.data.model.enums.MessageType
import org.vitrivr.vitrivrapp.data.model.enums.QueryTermType

data class QueryTermModel(var data: String, val categories: ArrayList<String>, var type: QueryTermType) : Parcelable {
    constructor(source: Parcel) : this(
            source.readString(),
            source.createStringArrayList(),
            QueryTermType.values()[source.readInt()]
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(data)
        writeStringList(categories)
        writeInt(type.ordinal)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<QueryTermModel> = object : Parcelable.Creator<QueryTermModel> {
            override fun createFromParcel(source: Parcel): QueryTermModel = QueryTermModel(source)
            override fun newArray(size: Int): Array<QueryTermModel?> = arrayOfNulls(size)
        }
    }
}

data class QueryContainerModel(@Transient val id: Long, @Transient var description: String, val terms: ArrayList<QueryTermModel>) : Parcelable {
    constructor(source: Parcel) : this(
            source.readLong(),
            source.readString(),
            ArrayList<QueryTermModel>().apply { source.readList(this, QueryTermModel::class.java.classLoader) }
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeLong(id)
        writeString(description)
        writeList(terms)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<QueryContainerModel> = object : Parcelable.Creator<QueryContainerModel> {
            override fun createFromParcel(source: Parcel): QueryContainerModel = QueryContainerModel(source)
            override fun newArray(size: Int): Array<QueryContainerModel?> = arrayOfNulls(size)
        }
    }
}

data class QueryModel(var messageType: MessageType, val containers: ArrayList<QueryContainerModel>) : Parcelable {
    constructor(source: Parcel) : this(
            MessageType.values()[source.readInt()],
            ArrayList<QueryContainerModel>().apply { source.readList(this, QueryContainerModel::class.java.classLoader) }
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt(messageType.ordinal)
        writeList(containers)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<QueryModel> = object : Parcelable.Creator<QueryModel> {
            override fun createFromParcel(source: Parcel): QueryModel = QueryModel(source)
            override fun newArray(size: Int): Array<QueryModel?> = arrayOfNulls(size)
        }
    }
}
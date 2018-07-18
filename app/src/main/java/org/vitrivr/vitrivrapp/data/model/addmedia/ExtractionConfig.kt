package org.vitrivr.vitrivrapp.data.model.addmedia

import android.os.Parcel
import android.os.Parcelable
import org.vitrivr.vitrivrapp.data.model.enums.MediaType

data class ExtractionConfig(val items: ArrayList<ExtractionItem> = ArrayList()) : Parcelable {
    constructor(source: Parcel) : this(
            source.createTypedArrayList(ExtractionItem.CREATOR)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeTypedList(items)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ExtractionConfig> = object : Parcelable.Creator<ExtractionConfig> {
            override fun createFromParcel(source: Parcel): ExtractionConfig = ExtractionConfig(source)
            override fun newArray(size: Int): Array<ExtractionConfig?> = arrayOfNulls(size)
        }
    }
}

data class ExtractionItem(val `object`: ExtractionObject, val metadata: ArrayList<ExtractionMetadata>, val uri: String = "file:///dummy") : Parcelable {
    constructor(source: Parcel) : this(
            source.readParcelable<ExtractionObject>(ExtractionObject::class.java.classLoader),
            source.createTypedArrayList(ExtractionMetadata.CREATOR),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeParcelable(`object`, 0)
        writeTypedList(metadata)
        writeString(uri)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ExtractionItem> = object : Parcelable.Creator<ExtractionItem> {
            override fun createFromParcel(source: Parcel): ExtractionItem = ExtractionItem(source)
            override fun newArray(size: Int): Array<ExtractionItem?> = arrayOfNulls(size)
        }
    }
}

data class ExtractionObject(val name: String, val path: String, val mediatype: MediaType) : Parcelable {
    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            MediaType.values()[source.readInt()]
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(name)
        writeString(path)
        writeInt(mediatype.ordinal)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ExtractionObject> = object : Parcelable.Creator<ExtractionObject> {
            override fun createFromParcel(source: Parcel): ExtractionObject = ExtractionObject(source)
            override fun newArray(size: Int): Array<ExtractionObject?> = arrayOfNulls(size)
        }
    }
}

data class ExtractionMetadata(var domain: String = "", var key: String = "", var value: String = "") : Parcelable {
    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(domain)
        writeString(key)
        writeString(value)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ExtractionMetadata> = object : Parcelable.Creator<ExtractionMetadata> {
            override fun createFromParcel(source: Parcel): ExtractionMetadata = ExtractionMetadata(source)
            override fun newArray(size: Int): Array<ExtractionMetadata?> = arrayOfNulls(size)
        }
    }
}
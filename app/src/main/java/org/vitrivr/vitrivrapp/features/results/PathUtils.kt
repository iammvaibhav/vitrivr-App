package org.vitrivr.vitrivrapp.features.results

import android.net.Uri
import org.vitrivr.vitrivrapp.App
import org.vitrivr.vitrivrapp.data.model.enums.MediaType
import org.vitrivr.vitrivrapp.data.model.results.QueryResultPresenterModel
import org.vitrivr.vitrivrapp.data.services.SettingsService
import java.io.File
import javax.inject.Inject

class PathUtils {

    @Inject
    lateinit var settingsService: SettingsService

    private val thumbnailsExtensions = hashMapOf(Pair(MediaType.IMAGE, "png"),
            Pair(MediaType.VIDEO, "png"),
            Pair(MediaType.AUDIO, "jpg"),
            Pair(MediaType.MODEL3D, "jpg"))

    init {
        App.daggerAppComponent.inject(this)
    }

    fun getObjectCompletePath(item: QueryResultPresenterModel): String? {
        val prePath = settingsService.getResourcesSettings()?.objectsURL ?: return null

        return when (item.mediaType) {
            MediaType.IMAGE -> "$prePath/image/${item.filePath}"
            MediaType.VIDEO -> "$prePath/video/${item.filePath}"
            MediaType.AUDIO -> "$prePath/audio/${item.filePath}"
            MediaType.MODEL3D -> "$prePath/model3d/${item.filePath}"
        }
    }

    fun getThumbnailCompletePath(item: QueryResultPresenterModel): String? {
        val prePath = settingsService.getResourcesSettings()?.thumbnailsURL ?: return null

        return when (item.mediaType) {
            MediaType.IMAGE -> "$prePath/image/${item.objectId}/${item.segmentDetail.segmentId}.${thumbnailsExtensions[item.mediaType]}"
            MediaType.VIDEO -> "$prePath/video/${item.objectId}/${item.segmentDetail.segmentId}.${thumbnailsExtensions[item.mediaType]}"
            MediaType.AUDIO -> "$prePath/audio/${item.objectId}/${item.segmentDetail.segmentId}.${thumbnailsExtensions[item.mediaType]}"
            MediaType.MODEL3D -> "$prePath/model3d/${item.objectId}/${item.segmentDetail.segmentId}.${thumbnailsExtensions[item.mediaType]}"
        }
    }

    fun getThumbnailOfSegment(mediaType: MediaType, objectId: String, segmentId: String): String? {
        val prePath = settingsService.getResourcesSettings()?.thumbnailsURL ?: return null

        return when (mediaType) {
            MediaType.IMAGE -> "$prePath/image/$objectId/$segmentId.${thumbnailsExtensions[mediaType]}"
            MediaType.VIDEO -> "$prePath/video/$objectId/$segmentId.${thumbnailsExtensions[mediaType]}"
            MediaType.AUDIO -> "$prePath/audio/$objectId/$segmentId.${thumbnailsExtensions[mediaType]}"
            MediaType.MODEL3D -> "$prePath/model3d/$objectId/$segmentId.${thumbnailsExtensions[mediaType]}"
        }
    }

    fun getObjectURI(presenterObject: QueryResultPresenterModel): Uri? {
        val path: String = getObjectCompletePath(presenterObject)!!

        if (isObjectPathLocal() == true) {
            return Uri.parse("file://${File(path).absolutePath}")
        } else if (isObjectPathLocal() == false) {
            return Uri.parse(path)
        }
        return null
    }

    fun isObjectPathLocal(): Boolean? {
        val prePath = settingsService.getResourcesSettings()?.objectsURL ?: return null
        return prePath[0] == '/'
    }

    fun isThumbnailPathLocal(): Boolean? {
        val prePath = settingsService.getResourcesSettings()?.thumbnailsURL ?: return null
        return prePath[0] == '/'
    }

    fun getFileOfObject(item: QueryResultPresenterModel): File? {
        if (isObjectPathLocal() == null || isObjectPathLocal() == false) return null
        return File(getObjectCompletePath(item))
    }

    fun getFileOfThumbnail(item: QueryResultPresenterModel): File? {
        if (isThumbnailPathLocal() == null || isThumbnailPathLocal() == false) return null
        return File(getThumbnailCompletePath(item))
    }

    fun getFileOfThumbnail(mediaType: MediaType, objectId: String, segmentId: String): File? {
        if (isThumbnailPathLocal() == null || isThumbnailPathLocal() == false) return null
        return File(getThumbnailOfSegment(mediaType, objectId, segmentId))
    }

}

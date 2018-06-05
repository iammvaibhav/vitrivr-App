package org.vitrivr.vitrivrapp.features.results

import org.vitrivr.vitrivrapp.App
import org.vitrivr.vitrivrapp.data.model.enums.MediaType
import org.vitrivr.vitrivrapp.data.model.results.QueryResultPresenterModel
import org.vitrivr.vitrivrapp.data.services.SettingsService
import java.io.File
import javax.inject.Inject

class PathUtils {

    @Inject
    lateinit var settingsService: SettingsService

    private val objectExtensions = hashMapOf(Pair(MediaType.IMAGE, "jpg"),
            Pair(MediaType.VIDEO, "mp4")
            /*,Pair(MediaType.AUDIO, "wav"),
            Pair(MediaType.MODEL3D, "obj")*/)
    //TODO("Adding for audio and model3d")

    private val thumbnailsExtensions = hashMapOf(Pair(MediaType.IMAGE, "png"),
            Pair(MediaType.VIDEO, "png")
            /*,Pair(MediaType.AUDIO, "wav"),
            Pair(MediaType.MODEL3D, "obj")*/)

    init {
        App.daggerAppComponent.inject(this)
    }

    fun getObjectCompletePath(item: QueryResultPresenterModel): String? {
        val prePath = settingsService.getResourcesSettings()?.objectsURL ?: return null

        return when (item.mediaType) {
            MediaType.IMAGE -> "$prePath/image/${item.filePath}.${objectExtensions[item.mediaType]}"
            MediaType.VIDEO -> "$prePath/video/${item.filePath}.${objectExtensions[item.mediaType]}"
            MediaType.AUDIO -> "$prePath/audio/${item.filePath}.${objectExtensions[item.mediaType]}"
            MediaType.MODEL3D -> "$prePath/model3d/${item.filePath}.${objectExtensions[item.mediaType]}"
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

}

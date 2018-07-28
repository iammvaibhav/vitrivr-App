package org.vitrivr.vitrivrapp.data.repository

import android.net.Uri
import org.vitrivr.vitrivrapp.data.helper.SharedPreferenceHelper
import org.vitrivr.vitrivrapp.data.model.enums.QueryTermType
import org.vitrivr.vitrivrapp.data.model.query.QueryModel

/**
 * QueryRepository is used for interacting with the SharedPreferences
 */
@Suppress("PrivatePropertyName")
class QueryRepository {

    private val PREF_NAME_QUERY_REPOSITORY = "PREF_NAME_QUERY_REPOSITORY"
    private val PREF_NAME_MOTION_DRAWING_DATA = "PREF_NAME_MOTION_DRAWING_DATA"

    private val QUERY_MODEL_KEY = "QUERY_MODEL_KEY"
    private val QUERY_CURR_CONTAINER_ID_KEY = "QUERY_CURR_CONTAINER_ID_KEY"
    private val QUERY_CURR_TERM_KEY = "QUERY_CURR_TERM_KEY"
    private val QUERY_3D_MODEL_URI = "QUERY_3D_MODEL_URI"

    private val spHelper = SharedPreferenceHelper(PREF_NAME_QUERY_REPOSITORY)
    private val motionSpHelper = SharedPreferenceHelper(PREF_NAME_MOTION_DRAWING_DATA)

    /**
     * put queryModel to persistent storage
     * @param queryModel queryModel to store
     */
    fun putQueryObject(queryModel: QueryModel) {
        spHelper.putObject(QUERY_MODEL_KEY, queryModel)
    }

    /**
     * @return QueryModel object if exists else null
     */
    fun getQueryObject(): QueryModel? {
        return spHelper.getObject(QUERY_MODEL_KEY, QueryModel::class.java)
    }

    /**
     * remove query object from persistent storage
     */
    fun removeQueryObject() {
        spHelper.removeKey(QUERY_MODEL_KEY)
    }

    /**
     * put currTermType in persistent storage
     * @param currTermType currTermType to store
     */
    fun putCurrTermType(currTermType: QueryTermType) {
        spHelper.putString(QUERY_CURR_TERM_KEY, currTermType.name)
    }

    /**
     * @return current QueryTermType if exist else null
     */
    fun getCurrTermType(): QueryTermType? {
        spHelper.getString(QUERY_CURR_TERM_KEY)?.let {
            return QueryTermType.valueOf(it)
        }
        return null
    }

    /**
     * remove current term type from persistent storage
     */
    fun removeCurrTermType() {
        spHelper.removeKey(QUERY_CURR_TERM_KEY)
    }

    /**
     * put currContainerID to persistent storage
     * @param currContainerID currContainerID to store
     */
    fun putCurrContainerID(currContainerID: Long) {
        spHelper.putLong(QUERY_CURR_CONTAINER_ID_KEY, currContainerID)
    }

    /**
     * @return current container ID if exist else null
     */
    fun getCurrContainerID(): Long? {
        return spHelper.getLong(QUERY_CURR_CONTAINER_ID_KEY)
    }

    /**
     * remove current container ID from persistent storage
     */
    fun removeCurrContainerID() {
        spHelper.removeKey(QUERY_CURR_CONTAINER_ID_KEY)
    }

    /**
     * put a model uri of a container to persistent storage
     * @param containerId containerID associated with the model
     * @param uri Uri to store
     */
    fun putModelUri(containerId: Long, uri: Uri?) {
        if (uri == null)
            spHelper.removeKey("${QUERY_3D_MODEL_URI}_$containerId")
        else spHelper.putString("${QUERY_3D_MODEL_URI}_$containerId", uri.toString())
    }

    /**
     * @param containerID ID of container to get Uri of
     * @return Uri of the model if exist, else return null
     */
    fun getModelUri(containerID: Long): Uri? {
        val uriString = spHelper.getString("${QUERY_3D_MODEL_URI}_$containerID") ?: return null
        return Uri.parse(uriString)
    }


    /**
     * remove the motion data associated with the containerID from the persistent storage
     * @param containerID ID of the container of which motion data is to be removed
     */
    @Suppress("LocalVariableName")
    fun removeMotionData(containerID: Long) {
        val PATH_LIST_SAVE = "PATH_LIST_SAVE_$containerID"
        val ARROW_LIST_SAVE = "ARROW_LIST_SAVE_$containerID"

        motionSpHelper.removeKey(PATH_LIST_SAVE)
        motionSpHelper.removeKey(ARROW_LIST_SAVE)
    }

}
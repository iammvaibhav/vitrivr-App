package org.vitrivr.vitrivrapp.data.repository

import android.content.Context
import android.net.Uri
import org.vitrivr.vitrivrapp.data.helper.SharedPreferenceHelper
import org.vitrivr.vitrivrapp.data.model.query.QueryModel
import javax.inject.Inject

class QueryRepository @Inject constructor(context: Context) {

    val QUERY_KEY = "QUERY_KEY"
    val QUERY_MODEL_KEY = "QUERY_MODEL_KEY"
    val QUERY_3D_MODEL_URI = "QUERY_3D_MODEL_URI"
    private val spHelper = SharedPreferenceHelper(context, QUERY_KEY)

    fun putQueryObject(queryModel: QueryModel) {
        spHelper.putObject(QUERY_MODEL_KEY, queryModel)
    }

    fun getQueryObject(): QueryModel? {
        return spHelper.getObject(QUERY_MODEL_KEY, QueryModel::class.java)
    }

    fun putModelUri(uri: Uri?, containerId: Long) {
        if (uri == null)
            spHelper.removeString("${QUERY_3D_MODEL_URI}_$containerId")
        else spHelper.putString("${QUERY_3D_MODEL_URI}_$containerId", uri.toString())
    }

    fun getModelUri(containerId: Long): Uri? {
        val uriString = spHelper.getString("${QUERY_3D_MODEL_URI}_$containerId") ?: return null
        return Uri.parse(uriString)
    }

}
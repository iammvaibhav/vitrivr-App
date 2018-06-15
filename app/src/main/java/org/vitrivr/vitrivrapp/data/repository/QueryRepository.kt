package org.vitrivr.vitrivrapp.data.repository

import android.content.Context
import org.vitrivr.vitrivrapp.data.helper.SharedPreferenceHelper
import org.vitrivr.vitrivrapp.data.model.query.QueryModel
import javax.inject.Inject

class QueryRepository @Inject constructor(context: Context) {

    val QUERY_KEY = "QUERY_KEY"
    val QUERY_MODEL_KEY = "QUERY_MODEL_KEY"
    private val spHelper = SharedPreferenceHelper(context, QUERY_KEY)

    fun putQueryObject(queryModel: QueryModel) {
        spHelper.putObject(QUERY_MODEL_KEY, queryModel)
    }

    fun getQueryObject(): QueryModel? {
        return spHelper.getObject(QUERY_MODEL_KEY, QueryModel::class.java)
    }

}
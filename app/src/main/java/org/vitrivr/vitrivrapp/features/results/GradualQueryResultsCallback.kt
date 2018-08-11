package org.vitrivr.vitrivrapp.features.results

import android.support.v7.util.DiffUtil
import org.vitrivr.vitrivrapp.data.model.results.QueryResultPresenterModel

/**
 * This DiffUtil Callback is used to calculate the difference between oldItems & newItems
 */
class GradualQueryResultsCallback(private val oldItems: List<QueryResultPresenterModel>,
                                  private val newItems: List<QueryResultPresenterModel>) : DiffUtil.Callback() {

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition].objectId == newItems[newItemPosition].objectId
    }

    override fun getOldListSize() = oldItems.size

    override fun getNewListSize() = newItems.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition] == newItems[newItemPosition]
    }
}
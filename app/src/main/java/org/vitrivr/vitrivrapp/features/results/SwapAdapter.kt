package org.vitrivr.vitrivrapp.features.results

import org.vitrivr.vitrivrapp.data.model.results.QueryResultPresenterModel

/**
 * Interface to swap the result items in the adapter
 */
@Suppress("LeakingThis")
interface SwapAdapter {
    fun swap(items: List<QueryResultPresenterModel>)
}
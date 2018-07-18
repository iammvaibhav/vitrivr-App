package org.vitrivr.vitrivrapp.features.addmedia

import android.support.v7.util.DiffUtil
import org.vitrivr.vitrivrapp.data.model.addmedia.ExtractionItem

class SelectedItemsChangeCallback(val oldItems: List<ExtractionItem>,
                                  val newItems: List<ExtractionItem>) : DiffUtil.Callback() {

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition].`object`.name == newItems[newItemPosition].`object`.name
    }

    override fun getOldListSize() = oldItems.size

    override fun getNewListSize() = newItems.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition] == newItems[newItemPosition]
    }
}
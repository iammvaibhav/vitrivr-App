package org.vitrivr.vitrivrapp.features.addmedia

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.data.model.addmedia.ExtractionConfig
import org.vitrivr.vitrivrapp.data.model.addmedia.ExtractionItem
import org.vitrivr.vitrivrapp.features.addmedia.SelectedItemsAdapter.Companion.SelectedItemViewHolder

/**
 * Adapter for showing selected items
 */
class SelectedItemsAdapter(private val config: ExtractionConfig) : RecyclerView.Adapter<SelectedItemViewHolder>() {

    private val items = mutableListOf<ExtractionItem>()

    init {
        items.addAll(config.items)
    }

    companion object {
        class SelectedItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val itemName: TextView = view.findViewById(R.id.itemName)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.add_media_selected_item, parent, false)
        return SelectedItemViewHolder(view)
    }

    override fun getItemCount() = config.items.size

    override fun onBindViewHolder(holder: SelectedItemViewHolder, position: Int) {
        holder.itemName.text = items[position].`object`.name
        holder.itemView.setOnClickListener {
            val dialog = MetadataEditorDialog(holder.itemView.context, items[holder.adapterPosition].`object`.name, items[holder.adapterPosition].metadata)
            dialog.show()
        }
    }

    fun update() {
        val diffCallback = SelectedItemsChangeCallback(this.items, config.items)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.items.clear()
        this.items.addAll(config.items)
        diffResult.dispatchUpdatesTo(this)
    }
}
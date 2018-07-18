package org.vitrivr.vitrivrapp.features.addmedia

import android.support.design.widget.TextInputEditText
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.data.model.addmedia.ExtractionMetadata
import org.vitrivr.vitrivrapp.features.addmedia.MetadataEditorAdapter.Companion.MetadataItemViewHolder

class MetadataEditorAdapter(val metadata: ArrayList<ExtractionMetadata>) : RecyclerView.Adapter<MetadataItemViewHolder>() {

    companion object {
        class MetadataItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val domain = view.findViewById<TextInputEditText>(R.id.domain)
            val key = view.findViewById<TextInputEditText>(R.id.key)
            val value = view.findViewById<TextInputEditText>(R.id.value)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetadataItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.metadata_item, parent, false)
        return MetadataItemViewHolder(view)
    }

    override fun getItemCount() = metadata.size

    override fun onBindViewHolder(holder: MetadataItemViewHolder, position: Int) {
        holder.domain.setText(metadata[position].domain)
        holder.key.setText(metadata[position].key)
        holder.value.setText(metadata[position].value)

        holder.domain.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.let { metadata[holder.adapterPosition].domain = it.toString() }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        holder.key.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.let { metadata[holder.adapterPosition].key = it.toString() }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        holder.value.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.let { metadata[holder.adapterPosition].value = it.toString() }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}
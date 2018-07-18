package org.vitrivr.vitrivrapp.features.addmedia

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.ViewGroup
import android.view.Window
import kotlinx.android.synthetic.main.metadata_editor_dialog.*
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.components.results.EqualSpacingItemDecoration
import org.vitrivr.vitrivrapp.data.model.addmedia.ExtractionMetadata
import org.vitrivr.vitrivrapp.utils.px


class MetadataEditorDialog(context: Context, val fileName: String, val metadata: ArrayList<ExtractionMetadata>) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.metadata_editor_dialog)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        if (metadata.isEmpty()) metadata.add(ExtractionMetadata())

        name.text = fileName
        metadataEditorRecyclerView.layoutManager = LinearLayoutManager(ownerActivity)
        metadataEditorRecyclerView.addItemDecoration(DividerItemDecoration(metadataEditorRecyclerView.context,
                DividerItemDecoration.VERTICAL))
        metadataEditorRecyclerView.addItemDecoration(EqualSpacingItemDecoration(6.px))
        metadataEditorRecyclerView.adapter = MetadataEditorAdapter(metadata)

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                metadata.removeAt(pos)
                metadataEditorRecyclerView.adapter.notifyItemRemoved(pos)
            }
        }

        // attaching the touch helper to recycler view
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(metadataEditorRecyclerView)

        add.setOnClickListener {
            metadata.add(ExtractionMetadata())
            metadataEditorRecyclerView.adapter.notifyDataSetChanged()
        }

        ok.setOnClickListener {
            dismiss()
        }
    }
}
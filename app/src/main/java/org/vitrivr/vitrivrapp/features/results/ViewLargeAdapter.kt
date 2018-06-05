package org.vitrivr.vitrivrapp.features.results

import android.graphics.Typeface
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.data.model.results.QueryResultPresenterModel
import org.vitrivr.vitrivrapp.utils.format


class ViewLargeAdapter(val items: List<QueryResultPresenterModel>, val directoryPath: String) : RecyclerView.Adapter<ViewLargeAdapter.Companion.ViewLarge_VH>() {

    companion object {
        class ViewLarge_VH(view: View) : RecyclerView.ViewHolder(view) {
            val fileName = view.findViewById<TextView>(R.id.fileName)
            val matchPercent = view.findViewById<TextView>(R.id.matchPercent)
            val previewThumbnail = view.findViewById<ImageView>(R.id.previewImage)
            val info = view.findViewById<ImageView>(R.id.info)
            val details = view.findViewById<ImageView>(R.id.details)
            val moreLikeThis = view.findViewById<ImageView>(R.id.moreLikeThis)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewLarge_VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.result_view_large, parent, false)
        return ViewLarge_VH(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewLarge_VH, position: Int) {
        holder.fileName.text = items[position].fileName
        holder.matchPercent.text = "${(items[position].segmentDetail.matchValue * 100).format(2)}%"

        val infoTextBuilder = StringBuilder()
        val pad = (items[position].segmentDetail.categoriesWeights.keys.map { it.length }.max()
                ?: 0) + 2
        for ((category, weight) in items[position].segmentDetail.categoriesWeights) {
            infoTextBuilder.append("${category.padEnd(pad)}: ${weight.format(3)}\n")
        }

        val featureInfoDialog = AlertDialog.Builder(holder.itemView.context)
                .setTitle("Feature Information")
                .setMessage(infoTextBuilder.toString())
                .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
                .create()

        holder.info.setOnClickListener {
            featureInfoDialog.show()
            featureInfoDialog.window.findViewById<TextView>(R.id.alertTitle).typeface = Typeface.MONOSPACE
            featureInfoDialog.window.findViewById<TextView>(android.R.id.message).typeface = Typeface.MONOSPACE
        }

        Picasso.get().load(directoryPath + items[position].filePath).into(holder.previewThumbnail)
    }
}
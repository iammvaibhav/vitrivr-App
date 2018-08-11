package org.vitrivr.vitrivrapp.features.results

import android.content.Intent
import android.graphics.Typeface
import android.support.v7.app.AlertDialog
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import org.vitrivr.vitrivrapp.App
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.data.model.enums.MediaType
import org.vitrivr.vitrivrapp.data.model.enums.MessageType
import org.vitrivr.vitrivrapp.data.model.enums.ResultViewType
import org.vitrivr.vitrivrapp.data.model.query.MoreLikeThisQueryModel
import org.vitrivr.vitrivrapp.data.model.results.QueryResultPresenterModel
import org.vitrivr.vitrivrapp.features.resultdetails.ImageResultDetailActivity
import org.vitrivr.vitrivrapp.features.resultdetails.Model3DResultDetailActivity
import org.vitrivr.vitrivrapp.features.resultdetails.VideoResultDetailActivity
import org.vitrivr.vitrivrapp.features.results.ResultsActivity.Companion.CATEGORY_INFO
import org.vitrivr.vitrivrapp.features.results.ResultsActivity.Companion.PRESENTER_OBJECT
import org.vitrivr.vitrivrapp.utils.PathUtils
import org.vitrivr.vitrivrapp.utils.format
import javax.inject.Inject

/**
 * ViewDetailsAdapter is a RecyclerView Adapter which is used to show result items along with details.
 * resultViewType can be MEDIUM or LARGE
 * resultsViewModel is used for accessing category count
 * startQuery is used to execute MLT query.
 */
class ViewDetailsAdapter(private val resultViewType: ResultViewType,
                         private val resultsViewModel: ResultsViewModel,
                         private val startQuery: (String) -> Unit) : RecyclerView.Adapter<ViewDetailsAdapter.Companion.ViewDetailVH>(), SwapAdapter {

    @Inject
    lateinit var pathUtils: PathUtils

    @Inject
    lateinit var gson: Gson

    /**
     * holds current result of items known to this adapter
     */
    private val items = mutableListOf<QueryResultPresenterModel>()

    init {
        App.daggerAppComponent.inject(this)
    }

    companion object {
        /**
         * ViewHolder class for this adapter
         */
        class ViewDetailVH(view: View) : RecyclerView.ViewHolder(view) {

            val fileName: TextView = view.findViewById(R.id.fileName)
            val matchPercent: TextView = view.findViewById(R.id.matchPercent)
            val previewThumbnail: ImageView = view.findViewById(R.id.previewImage)
            val info: ImageView = view.findViewById(R.id.info)
            val details: ImageView = view.findViewById(R.id.details)
            val moreLikeThis: ImageView = view.findViewById(R.id.moreLikeThis)

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewDetailVH {
        /**
         * based on the resultViewType, inflate the correct layout
         */
        val layout = if (resultViewType == ResultViewType.LARGE) R.layout.result_view_large else R.layout.result_view_medium
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewDetailVH(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewDetailVH, position: Int) {
        holder.fileName.text = items[position].fileName

        holder.matchPercent.text = String.format("%.2f%s", items[position].segmentDetail.matchValue * 100, "%")

        /**
         * constructing dialog to show feature information
         */
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

        if (pathUtils.isThumbnailPathLocal()) {
            /**
             * Thumbnails stored locally.
             */
            Picasso.get()
                    .load(pathUtils.getFileObjectForThumbnail(items[position]))
                    .fit()
                    .centerCrop()
                    .into(holder.previewThumbnail)
        } else {
            /**
             * Thumbnails to be fetched from Network.
             */
            Picasso.get()
                    .load(pathUtils.getThumbnailCompletePath(items[position]))
                    .fit()
                    .centerCrop()
                    .into(holder.previewThumbnail)
        }

        /**
         * Construct a MoreLikeThisQueryModel object and start the query
         */
        holder.moreLikeThis.setOnClickListener {
            val mltQuery = MoreLikeThisQueryModel(items[position].segmentDetail.segmentId,
                    ArrayList(resultsViewModel.categoryCount[items[position].mediaType]!!.toList()),
                    MessageType.Q_MLT)
            startQuery(gson.toJson(mltQuery))
        }

        val openDetailsListener = View.OnClickListener {
            val intent = when (items[position].mediaType) {
                MediaType.IMAGE -> Intent(holder.itemView.context, ImageResultDetailActivity::class.java)
                MediaType.VIDEO -> Intent(holder.itemView.context, VideoResultDetailActivity::class.java)
                MediaType.AUDIO -> Intent(holder.itemView.context, VideoResultDetailActivity::class.java)
                MediaType.MODEL3D -> Intent(holder.itemView.context, Model3DResultDetailActivity::class.java)
            }

            intent.putExtra(PRESENTER_OBJECT, items[position])
            intent.putExtra(CATEGORY_INFO, resultsViewModel.categoryCount)
            holder.itemView.context.startActivity(intent)
        }

        /**
         * open the appropriate details activity according to the media type
         */
        holder.previewThumbnail.setOnClickListener(openDetailsListener)
        holder.details.setOnClickListener(openDetailsListener)
    }

    /**
     * swap the current result list with the list provided
     * @param items list of new results
     */
    override fun swap(items: List<QueryResultPresenterModel>) {
        val itemsToTake = items.filter { it.visibility }
        val diffCallback = GradualQueryResultsCallback(this.items, itemsToTake)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.items.clear()
        this.items.addAll(itemsToTake)
        diffResult.dispatchUpdatesTo(this)
    }
}
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
import org.vitrivr.vitrivrapp.utils.format
import javax.inject.Inject


class ViewDetailsAdapter(initItemsList: List<QueryResultPresenterModel>,
                         private val resultViewType: ResultViewType,
                         val resultsViewModel: ResultsViewModel,
                         val startQuery: (String) -> Unit) : RecyclerView.Adapter<ViewDetailsAdapter.Companion.ViewDetailVH>() {

    @Inject
    lateinit var pathUtils: PathUtils
    @Inject
    lateinit var gson: Gson
    private val items = mutableListOf<QueryResultPresenterModel>()

    init {
        App.daggerAppComponent.inject(this)
        items.addAll(initItemsList.filter { it.visibility })
    }

    companion object {
        class ViewDetailVH(view: View) : RecyclerView.ViewHolder(view) {
            val fileName = view.findViewById<TextView>(R.id.fileName)
            val matchPercent = view.findViewById<TextView>(R.id.matchPercent)
            val previewThumbnail = view.findViewById<ImageView>(R.id.previewImage)
            val info = view.findViewById<ImageView>(R.id.info)
            val details = view.findViewById<ImageView>(R.id.details)
            val moreLikeThis = view.findViewById<ImageView>(R.id.moreLikeThis)
        }

        const val PRESENTER_OBJECT = "PRESENTER_OBJECT"
        const val CATEGORY_INFO = "CATEGORY_INFO"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewDetailVH {
        val layout = if (resultViewType == ResultViewType.LARGE) R.layout.result_view_large else R.layout.result_view_medium
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewDetailVH(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewDetailVH, position: Int) {
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

        if (pathUtils.isThumbnailPathLocal() == true) {
            pathUtils.getFileOfThumbnail(items[position])?.let {
                Picasso.get()
                        .load(it)
                        .fit()
                        .centerCrop()
                        .into(holder.previewThumbnail)
            }
        } else if (pathUtils.isThumbnailPathLocal() == false) {
            pathUtils.getThumbnailCompletePath(items[position])?.let {
                Picasso.get()
                        .load(it)
                        .fit()
                        .centerCrop()
                        .into(holder.previewThumbnail)
            }
        }

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

        holder.previewThumbnail.setOnClickListener(openDetailsListener)
        holder.details.setOnClickListener(openDetailsListener)
    }

    fun swap(items: List<QueryResultPresenterModel>) {
        val itemsToTake = items.filter { it.visibility }
        val diffCallback = GradualQueryResultsCallback(this.items, itemsToTake)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.items.clear()
        this.items.addAll(itemsToTake)
        diffResult.dispatchUpdatesTo(this)
    }
}
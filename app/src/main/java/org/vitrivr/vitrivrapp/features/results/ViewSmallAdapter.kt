package org.vitrivr.vitrivrapp.features.results

import android.content.Intent
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.squareup.picasso.Picasso
import org.vitrivr.vitrivrapp.App
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.data.model.enums.MediaType
import org.vitrivr.vitrivrapp.data.model.results.QueryResultPresenterModel
import org.vitrivr.vitrivrapp.features.resultdetails.ImageResultDetailActivity
import org.vitrivr.vitrivrapp.features.resultdetails.Model3DResultDetailActivity
import org.vitrivr.vitrivrapp.features.resultdetails.VideoResultDetailActivity
import org.vitrivr.vitrivrapp.features.results.ResultsActivity.Companion.CATEGORY_INFO
import org.vitrivr.vitrivrapp.features.results.ResultsActivity.Companion.PRESENTER_OBJECT
import org.vitrivr.vitrivrapp.utils.PathUtils
import javax.inject.Inject

/**
 * ViewSmallAdapter is a RecyclerView Adapter which is used to show result items (only preview) without the details.
 * resultsViewModel is used for accessing category count
 */
class ViewSmallAdapter(private val resultsViewModel: ResultsViewModel) : RecyclerView.Adapter<ViewSmallAdapter.Companion.ViewSmallVH>(), SwapAdapter {

    @Inject
    lateinit var pathUtils: PathUtils

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
        class ViewSmallVH(view: View) : RecyclerView.ViewHolder(view) {
            val previewThumbnail: ImageView = view.findViewById(R.id.previewImage)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewSmallVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.result_view_small, parent, false)
        return ViewSmallVH(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewSmallVH, position: Int) {
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
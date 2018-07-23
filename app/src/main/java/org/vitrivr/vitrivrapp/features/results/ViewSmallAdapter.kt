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
import javax.inject.Inject

class ViewSmallAdapter(initItemsList: List<QueryResultPresenterModel>, val resultsViewModel: ResultsViewModel) : RecyclerView.Adapter<ViewSmallAdapter.Companion.ViewSmallVH>() {

    @Inject
    lateinit var pathUtils: PathUtils
    private val items = mutableListOf<QueryResultPresenterModel>()

    init {
        App.daggerAppComponent.inject(this)
        items.addAll(initItemsList.filter { it.visibility })
    }

    companion object {
        class ViewSmallVH(view: View) : RecyclerView.ViewHolder(view) {
            val previewThumbnail = view.findViewById<ImageView>(R.id.previewImage)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewSmallVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.result_view_small, parent, false)
        return ViewSmallVH(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewSmallVH, position: Int) {
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

        val openDetailsListener = View.OnClickListener {
            val intent = when (items[position].mediaType) {
                MediaType.IMAGE -> Intent(holder.itemView.context, ImageResultDetailActivity::class.java)
                MediaType.VIDEO -> Intent(holder.itemView.context, VideoResultDetailActivity::class.java)
                MediaType.AUDIO -> Intent(holder.itemView.context, VideoResultDetailActivity::class.java)
                MediaType.MODEL3D -> Intent(holder.itemView.context, Model3DResultDetailActivity::class.java)
            }

            intent.putExtra(ViewDetailsAdapter.PRESENTER_OBJECT, items[position])
            intent.putExtra(ViewDetailsAdapter.CATEGORY_INFO, resultsViewModel.categoryCount)
            holder.itemView.context.startActivity(intent)
        }

        holder.previewThumbnail.setOnClickListener(openDetailsListener)
    }

    fun swap(items: List<QueryResultPresenterModel>) {
        val itemsToTake = items.filter { it.visibility }
        val diffCallback = GradualQueryResultsCallback(this.items, itemsToTake)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.items.clear()
        this.items.addAll(items)
        diffResult.dispatchUpdatesTo(this)
    }
}
package org.vitrivr.vitrivrapp.features.results

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.squareup.picasso.Picasso
import org.vitrivr.vitrivrapp.App
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.data.model.results.QueryResultPresenterModel
import javax.inject.Inject

class ViewSmallAdapter(val items: List<QueryResultPresenterModel>) : RecyclerView.Adapter<ViewSmallAdapter.Companion.ViewSmallVH>() {

    @Inject
    lateinit var pathUtils: PathUtils

    init {
        App.daggerAppComponent.inject(this)
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
            return
        }

        pathUtils.getThumbnailCompletePath(items[position])?.let {
            Picasso.get()
                    .load(it)
                    .fit()
                    .centerCrop()
                    .into(holder.previewThumbnail)
        }
    }
}
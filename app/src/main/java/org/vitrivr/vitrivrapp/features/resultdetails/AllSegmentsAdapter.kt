package org.vitrivr.vitrivrapp.features.resultdetails

import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import org.vitrivr.vitrivrapp.App
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.data.model.enums.MediaType
import org.vitrivr.vitrivrapp.data.model.enums.MessageType
import org.vitrivr.vitrivrapp.data.model.query.MoreLikeThisQueryModel
import org.vitrivr.vitrivrapp.data.model.results.SegmentDetails
import org.vitrivr.vitrivrapp.features.results.ResultsActivity
import org.vitrivr.vitrivrapp.features.results.ResultsActivity.Companion.QUERY_TYPE
import org.vitrivr.vitrivrapp.utils.PathUtils
import javax.inject.Inject

/**
 * Recycler View adapter for showing all segments
 */
class AllSegmentsAdapter(private val allSegments: List<SegmentDetails>,
                         private val objectId: String,
                         val mediaType: MediaType,
                         private val categoryInfo: HashMap<MediaType, HashSet<String>>,
                         private val segmentClickListener: ((Double) -> Unit)? = null) : RecyclerView.Adapter<AllSegmentsAdapter.Companion.AllSegmentsVH>() {

    @Inject
    lateinit var pathUtils: PathUtils
    @Inject
    lateinit var gson: Gson

    init {
        App.daggerAppComponent.inject(this)
    }

    companion object {
        class AllSegmentsVH(view: View) : RecyclerView.ViewHolder(view) {
            val previewImage: ImageView = view.findViewById(R.id.previewImage)
            val moreLikeThis: ImageView = view.findViewById(R.id.moreLikeThis)
            val play: ImageView = view.findViewById(R.id.play)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllSegmentsVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.segment_mlt_play, parent, false)
        val vh = AllSegmentsVH(view)
        when (mediaType) {
            MediaType.VIDEO -> vh.play.visibility = View.VISIBLE
            MediaType.AUDIO -> vh.play.visibility = View.VISIBLE
        }
        return vh
    }

    override fun getItemCount() = allSegments.size

    override fun onBindViewHolder(holder: AllSegmentsVH, position: Int) {

        if (pathUtils.isThumbnailPathLocal()) {
            Picasso.get()
                    .load(pathUtils.getFileObjectForThumbnail(mediaType, objectId, allSegments[position].segmentId))
                    .fit()
                    .centerCrop()
                    .into(holder.previewImage)
        } else {
            Picasso.get()
                    .load(pathUtils.getThumbnailOfSegment(mediaType, objectId, allSegments[position].segmentId))
                    .fit()
                    .centerCrop()
                    .into(holder.previewImage)
        }

        holder.moreLikeThis.setOnClickListener {
            val intent = Intent(holder.itemView.context, ResultsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val mltQuery = MoreLikeThisQueryModel(allSegments[position].segmentId,
                    ArrayList(categoryInfo[mediaType]!!),
                    MessageType.Q_MLT)
            intent.putExtra("query", gson.toJson(mltQuery))
            intent.putExtra(QUERY_TYPE, MessageType.Q_MLT)
            holder.itemView.context.startActivity(intent)
        }

        when (mediaType) {
            MediaType.VIDEO -> {
                holder.play.setOnClickListener {
                    segmentClickListener?.invoke(allSegments[position].startAbs)
                }
            }
            MediaType.AUDIO -> {
                holder.play.setOnClickListener {
                    segmentClickListener?.invoke(allSegments[position].startAbs)
                }
            }
        }
    }
}
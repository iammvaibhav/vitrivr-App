package org.vitrivr.vitrivrapp.features.query

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.query_toggles.view.*
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.data.model.enums.QueryTermType

/**
 * QueryToggles is a view which represent toggle group of QueryTermType
 */
class QueryToggles @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /**
     * get the required colors from resources
     */
    private val tileSelectedColor = ContextCompat.getColor(context, R.color.tileSelected)
    private val tileNormalColor = ContextCompat.getColor(context, R.color.tileNormal)

    /**
     * query term click listener which gets invoked when a particular queryTerm is clicked.
     * wasChecked value tells if it was checked before clicking
     */
    private var queryTermClickListener: ((queryTerm: QueryTermType, wasChecked: Boolean) -> Unit)? = null

    init {
        /**
         * inflate query_toggles layout
         */
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.query_toggles, this)

        /**
         * set click listeners for query terms.
         * obtain current check status, check them if they are not and invoke the queryTermClickListener
         * with respective QueryTermType and wasCheck status
         */
        queryImage.setOnClickListener {
            val wasChecked = isChecked(QueryTermType.IMAGE)
            if (!isChecked(QueryTermType.IMAGE)) setChecked(QueryTermType.IMAGE, true)
            queryTermClickListener?.invoke(QueryTermType.IMAGE, wasChecked)
        }

        queryAudio.setOnClickListener {
            val wasChecked = isChecked(QueryTermType.AUDIO)
            if (!isChecked(QueryTermType.AUDIO)) setChecked(QueryTermType.AUDIO, true)
            queryTermClickListener?.invoke(QueryTermType.AUDIO, wasChecked)
        }

        query3D.setOnClickListener {
            val wasChecked = isChecked(QueryTermType.MODEL3D)
            if (!isChecked(QueryTermType.MODEL3D)) setChecked(QueryTermType.MODEL3D, true)
            queryTermClickListener?.invoke(QueryTermType.MODEL3D, wasChecked)
        }

        queryMotion.setOnClickListener {
            val wasChecked = isChecked(QueryTermType.MOTION)
            if (!isChecked(QueryTermType.MOTION)) setChecked(QueryTermType.MOTION, true)
            queryTermClickListener?.invoke(QueryTermType.MOTION, wasChecked)
        }

        queryText.setOnClickListener {
            val wasChecked = isChecked(QueryTermType.TEXT)
            if (!isChecked(QueryTermType.TEXT)) setChecked(QueryTermType.TEXT, true)
            queryTermClickListener?.invoke(QueryTermType.TEXT, wasChecked)
        }

        queryLocation.setOnClickListener {
            val wasChecked = isChecked(QueryTermType.LOCATION)
            if (!isChecked(QueryTermType.LOCATION)) setChecked(QueryTermType.LOCATION, true)
            queryTermClickListener?.invoke(QueryTermType.LOCATION, wasChecked)
        }
    }

    /**
     * add the query term listener
     * @param queryTermClickListener listener
     */
    fun addQueryTermClickListener(queryTermClickListener: ((queryTerm: QueryTermType, wasChecked: Boolean) -> Unit)?) {
        this.queryTermClickListener = queryTermClickListener
    }

    /**
     * set the check status of QueryTermType 'type' according to checked
     * @param type QueryTermType to change check status for
     * @param checked check if true, else uncheck
     */
    fun setChecked(type: QueryTermType, checked: Boolean) {
        when (type) {
            QueryTermType.IMAGE -> if (checked) queryImage.setBackgroundColor(tileSelectedColor) else queryImage.setBackgroundColor(tileNormalColor)
            QueryTermType.AUDIO -> if (checked) queryAudio.setBackgroundColor(tileSelectedColor) else queryAudio.setBackgroundColor(tileNormalColor)
            QueryTermType.MODEL3D -> if (checked) query3D.setBackgroundColor(tileSelectedColor) else query3D.setBackgroundColor(tileNormalColor)
            QueryTermType.MOTION -> if (checked) queryMotion.setBackgroundColor(tileSelectedColor) else queryMotion.setBackgroundColor(tileNormalColor)
            QueryTermType.TEXT -> if (checked) queryText.setBackgroundColor(tileSelectedColor) else queryText.setBackgroundColor(tileNormalColor)
            QueryTermType.LOCATION -> if (checked) queryLocation.setBackgroundColor(tileSelectedColor) else queryLocation.setBackgroundColor(tileNormalColor)
        }
    }

    /**
     * programmatically click the provided query term
     * @param type QueryTermType to be clicked programmatically
     */
    fun performClick(type: QueryTermType) {
        when (type) {
            QueryTermType.IMAGE -> queryImage.performClick()
            QueryTermType.AUDIO -> queryAudio.performClick()
            QueryTermType.MODEL3D -> query3D.performClick()
            QueryTermType.MOTION -> queryMotion.performClick()
            QueryTermType.TEXT -> queryText.performClick()
            QueryTermType.LOCATION -> queryLocation.performClick()
        }
    }

    /**
     * checks if given QueryTermType is checked
     * @param type QueryTermType of which check status to check for
     * @return Boolean representing check status
     */
    private fun isChecked(type: QueryTermType) = when (type) {
        QueryTermType.IMAGE -> (queryImage.background as ColorDrawable).color == tileSelectedColor
        QueryTermType.AUDIO -> (queryAudio.background as ColorDrawable).color == tileSelectedColor
        QueryTermType.MODEL3D -> (query3D.background as ColorDrawable).color == tileSelectedColor
        QueryTermType.MOTION -> (queryMotion.background as ColorDrawable).color == tileSelectedColor
        QueryTermType.TEXT -> (queryText.background as ColorDrawable).color == tileSelectedColor
        QueryTermType.LOCATION -> (queryLocation.background as ColorDrawable).color == tileSelectedColor
    }
}
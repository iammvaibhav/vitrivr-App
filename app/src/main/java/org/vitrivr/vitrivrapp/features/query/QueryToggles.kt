package org.vitrivr.vitrivrapp.features.query

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.query_toggles.view.*
import org.vitrivr.vitrivrapp.R


class QueryToggles @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class QueryTerm {
        IMAGE, AUDIO, MODEL3D, MOTION, TEXT, LOCATION
    }

    val tileSelectedColor: Int
    val tileNormalColor: Int

    private var queryTermClickListener: ((queryTerm: QueryTerm, wasChecked: Boolean) -> Unit)? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.query_toggles, this)

        tileSelectedColor = ContextCompat.getColor(context, R.color.tileSelected)
        tileNormalColor = ContextCompat.getColor(context, R.color.tileNormal)

        queryImage.setOnClickListener {
            val wasChecked = isChecked(QueryTerm.IMAGE)
            if (!isChecked(QueryTerm.IMAGE)) setChecked(QueryTerm.IMAGE, true)
            queryTermClickListener?.invoke(QueryTerm.IMAGE, wasChecked)
        }

        queryAudio.setOnClickListener {
            val wasChecked = isChecked(QueryTerm.AUDIO)
            if (!isChecked(QueryTerm.AUDIO)) setChecked(QueryTerm.AUDIO, true)
            queryTermClickListener?.invoke(QueryTerm.AUDIO, wasChecked)
        }

        query3D.setOnClickListener {
            val wasChecked = isChecked(QueryTerm.MODEL3D)
            if (!isChecked(QueryTerm.MODEL3D)) setChecked(QueryTerm.MODEL3D, true)
            queryTermClickListener?.invoke(QueryTerm.MODEL3D, wasChecked)
        }

        queryMotion.setOnClickListener {
            val wasChecked = isChecked(QueryTerm.MOTION)
            if (!isChecked(QueryTerm.MOTION)) setChecked(QueryTerm.MOTION, true)
            queryTermClickListener?.invoke(QueryTerm.MOTION, wasChecked)
        }

        queryText.setOnClickListener {
            val wasChecked = isChecked(QueryTerm.TEXT)
            if (!isChecked(QueryTerm.TEXT)) setChecked(QueryTerm.TEXT, true)
            queryTermClickListener?.invoke(QueryTerm.TEXT, wasChecked)
        }

        queryLocation.setOnClickListener {
            val wasChecked = isChecked(QueryTerm.LOCATION)
            if (!isChecked(QueryTerm.LOCATION)) setChecked(QueryTerm.LOCATION, true)
            queryTermClickListener?.invoke(QueryTerm.LOCATION, wasChecked)
        }
    }

    fun addQueryTermClickListener(queryTermClickListener: ((queryTerm: QueryTerm, wasChecked: Boolean) -> Unit)?) {
        this.queryTermClickListener = queryTermClickListener
    }

    fun checkedStatus() = arrayOf(isChecked(QueryTerm.IMAGE), isChecked(QueryTerm.AUDIO), isChecked(QueryTerm.MODEL3D),
            isChecked(QueryTerm.MOTION), isChecked(QueryTerm.TEXT), isChecked(QueryTerm.LOCATION))

    fun isChecked(type: QueryTerm) = when (type) {
        QueryTerm.IMAGE -> (queryImage.background as ColorDrawable).color == tileSelectedColor
        QueryTerm.AUDIO -> (queryAudio.background as ColorDrawable).color == tileSelectedColor
        QueryTerm.MODEL3D -> (query3D.background as ColorDrawable).color == tileSelectedColor
        QueryTerm.MOTION -> (queryMotion.background as ColorDrawable).color == tileSelectedColor
        QueryTerm.TEXT -> (queryText.background as ColorDrawable).color == tileSelectedColor
        QueryTerm.LOCATION -> (queryLocation.background as ColorDrawable).color == tileSelectedColor
    }

    fun setChecked(type: QueryTerm, checked: Boolean) {
        when (type) {
            QueryTerm.IMAGE -> if (checked) queryImage.setBackgroundColor(tileSelectedColor) else queryImage.setBackgroundColor(tileNormalColor)
            QueryTerm.AUDIO -> if (checked) queryAudio.setBackgroundColor(tileSelectedColor) else queryAudio.setBackgroundColor(tileNormalColor)
            QueryTerm.MODEL3D -> if (checked) query3D.setBackgroundColor(tileSelectedColor) else query3D.setBackgroundColor(tileNormalColor)
            QueryTerm.MOTION -> if (checked) queryMotion.setBackgroundColor(tileSelectedColor) else queryMotion.setBackgroundColor(tileNormalColor)
            QueryTerm.TEXT -> if (checked) queryText.setBackgroundColor(tileSelectedColor) else queryText.setBackgroundColor(tileNormalColor)
            QueryTerm.LOCATION -> if (checked) queryLocation.setBackgroundColor(tileSelectedColor) else queryLocation.setBackgroundColor(tileNormalColor)
        }
    }

    fun performClick(type: QueryTerm) {
        when (type) {
            QueryTerm.IMAGE -> queryImage.performClick()
            QueryTerm.AUDIO -> queryAudio.performClick()
            QueryTerm.MODEL3D -> query3D.performClick()
            QueryTerm.MOTION -> queryMotion.performClick()
            QueryTerm.TEXT -> queryText.performClick()
            QueryTerm.LOCATION -> queryLocation.performClick()
        }
    }
}
package org.vitrivr.vitrivrapp.features.query

import android.content.Context
import android.support.v7.widget.CardView
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import kotlinx.android.synthetic.main.query_container.view.*
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.features.query.QueryToggles.QueryTerm


class QueryContainer @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    private var deleteQueryContainerListener: (() -> Unit)? = null
    private var queryTermClickListener: ((queryTerm: QueryTerm, wasChecked: Boolean) -> Unit)? = null
    private var queryDescriptionChangeListener: ((description: String) -> Unit)? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.query_container, this)

        deleteContainer.setOnClickListener { deleteQueryContainerListener?.invoke() }

        queryToggles.addQueryTermClickListener { queryTerm, wasChecked ->
            queryTermClickListener?.invoke(queryTerm, wasChecked)
        }

        queryDescription.isSaveEnabled = false
        queryDescription.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.let { queryDescriptionChangeListener?.invoke(it.toString()) }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })
    }

    fun addDeleteQueryContainerListener(deleteQueryContainerListener: (() -> Unit)?) {
        this.deleteQueryContainerListener = deleteQueryContainerListener
    }

    fun addQueryTermClickListener(queryTermClickListener: ((queryTerm: QueryTerm, wasChecked: Boolean) -> Unit)?) {
        this.queryTermClickListener = queryTermClickListener
    }

    fun addQueryDescriptionChangeListener(queryDescriptionChangeListener: ((description: String) -> Unit)?) {
        this.queryDescriptionChangeListener = queryDescriptionChangeListener
    }

    fun checkedStatus() = arrayOf(isChecked(QueryTerm.IMAGE), isChecked(QueryTerm.AUDIO), isChecked(QueryTerm.MODEL3D),
            isChecked(QueryTerm.MOTION), isChecked(QueryTerm.TEXT), isChecked(QueryTerm.LOCATION))

    fun isChecked(type: QueryTerm) = queryToggles.isChecked(type)

    fun setChecked(type: QueryTerm, checked: Boolean) {
        queryToggles.setChecked(type, checked)
    }

    fun performClick(type: QueryTerm) {
        queryToggles.performClick(type)
    }

    fun setQueryDescription(description: String) {
        queryDescription.setText(description)
    }
}
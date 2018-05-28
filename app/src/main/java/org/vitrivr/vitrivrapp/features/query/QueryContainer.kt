package org.vitrivr.vitrivrapp.features.query

import android.content.Context
import android.support.v7.widget.CardView
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import kotlinx.android.synthetic.main.query_container.view.*
import org.vitrivr.vitrivrapp.R

class QueryContainer @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    enum class QueryTerm {
        IMAGE, AUDIO, THREE_D, MOTION, TEXT, LOCATION
    }

    private var deleteQueryContainerListener: (() -> Unit)? = null
    private var queryTermToggleListener: ((queryTerm: QueryTerm, checked: Boolean) -> Unit)? = null
    private var queryDescriptionChangeListener: ((description: String) -> Unit)? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.query_container, this)

        deleteContainer.setOnClickListener { deleteQueryContainerListener?.invoke() }

        queryImage.setOnCheckedChangeListener { _, isChecked -> queryTermToggleListener?.invoke(QueryTerm.IMAGE, isChecked) }
        queryAudio.setOnCheckedChangeListener { _, isChecked -> queryTermToggleListener?.invoke(QueryTerm.AUDIO, isChecked) }
        query3D.setOnCheckedChangeListener { _, isChecked -> queryTermToggleListener?.invoke(QueryTerm.THREE_D, isChecked) }
        queryMotion.setOnCheckedChangeListener { _, isChecked -> queryTermToggleListener?.invoke(QueryTerm.MOTION, isChecked) }
        queryText.setOnCheckedChangeListener { _, isChecked -> queryTermToggleListener?.invoke(QueryTerm.TEXT, isChecked) }
        queryLocation.setOnCheckedChangeListener { _, isChecked -> queryTermToggleListener?.invoke(QueryTerm.LOCATION, isChecked) }

        queryDescription.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.let { queryDescriptionChangeListener?.invoke(it.toString()) }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })
    }

    fun addDeleteQueryContainerListener(deleteQueryContainerListener: () -> Unit) {
        this.deleteQueryContainerListener = deleteQueryContainerListener
    }

    fun addQueryTermToggleListener(queryTermToggleListener: (queryTerm: QueryTerm, checked: Boolean) -> Unit) {
        this.queryTermToggleListener = queryTermToggleListener
    }

    fun addQueryDescriptionChangeListener(queryDescriptionChangeListener: (description: String) -> Unit) {
        this.queryDescriptionChangeListener = queryDescriptionChangeListener
    }

    fun removeDeleteQueryContainerListener() {
        this.deleteQueryContainerListener = null
    }

    fun removeAddQueryTermToggleListener() {
        this.queryTermToggleListener = null
    }

    fun removeQueryDescriptionChangeListener() {
        this.queryDescriptionChangeListener = null
    }

    fun clearAllFields() {
        queryDescription.setText("")
        queryImage.isChecked = false
        queryAudio.isChecked = false
        query3D.isChecked = false
        queryMotion.isChecked = false
        queryText.isChecked = false
        queryLocation.isChecked = false
    }

}
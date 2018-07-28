package org.vitrivr.vitrivrapp.features.query

import android.content.Context
import android.support.v7.widget.CardView
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import kotlinx.android.synthetic.main.query_container.view.*
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.data.model.enums.QueryTermType
import org.vitrivr.vitrivrapp.utils.px

/**
 * QueryContainer is a view representing a query container with all the query terms, description and delete button
 */
class QueryContainer @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    /**
     * listener for delete query bottom which is invoked when user clicks delete query button
     */
    private var deleteQueryContainerListener: (() -> Unit)? = null

    /**
     * listener for query term which is invoked when user clicks a particular query term.
     * It gets called with queryTerm, the QueryTermType of the query term user clicks and
     * wasChecked which represents if the term user clicked was previously checked.
     */
    private var queryTermClickListener: ((queryTerm: QueryTermType, wasChecked: Boolean) -> Unit)? = null

    /**
     * listener for description which is invoked when user changes the description.
     * It gets called with description, the changed description.
     */
    private var queryDescriptionChangeListener: ((description: String) -> Unit)? = null

    init {
        /**
         * Inflate the query_container layout
         */
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.query_container, this)

        /**
         * add listeners
         */
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

        val layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(10.px, 10.px, 10.px, 10.px)
        this.layoutParams = layoutParams
    }

    /**
     * add the delete query listener
     */
    fun addDeleteQueryContainerListener(deleteQueryContainerListener: (() -> Unit)?) {
        this.deleteQueryContainerListener = deleteQueryContainerListener
    }

    /**
     * add the query term listener
     * @param queryTermClickListener listener
     */
    fun addQueryTermClickListener(queryTermClickListener: ((queryTerm: QueryTermType, wasChecked: Boolean) -> Unit)?) {
        this.queryTermClickListener = queryTermClickListener
    }

    /**
     * add the query description listener
     * @param queryDescriptionChangeListener listener
     */
    fun addQueryDescriptionChangeListener(queryDescriptionChangeListener: ((description: String) -> Unit)?) {
        this.queryDescriptionChangeListener = queryDescriptionChangeListener
    }

    /**
     * set the check status of QueryTermType 'type' according to checked
     * @param type QueryTermType to change check status for
     * @param checked check if true, else uncheck
     */
    fun setChecked(type: QueryTermType, checked: Boolean) {
        queryToggles.setChecked(type, checked)
    }

    /**
     * programmatically click the provided query term
     * @param type QueryTermType to be clicked programmatically
     */
    fun performClick(type: QueryTermType) {
        queryToggles.performClick(type)
    }

    /**
     * sets the description
     * @param description description to be set
     */
    fun setQueryDescription(description: String) {
        queryDescription.setText(description)
    }
}
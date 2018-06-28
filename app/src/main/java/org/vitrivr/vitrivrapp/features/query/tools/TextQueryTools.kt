package org.vitrivr.vitrivrapp.features.query.tools

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.data.model.enums.QueryTermType
import org.vitrivr.vitrivrapp.features.query.QueryViewModel


class TextQueryTools @JvmOverloads constructor(val queryViewModel: QueryViewModel,
                                               wasChecked: Boolean,
                                               toolsContainer: ViewGroup,
                                               context: Context,
                                               attrs: AttributeSet? = null,
                                               defStyleAttr: Int = 0,
                                               defStyleRes: Int = 0) : View(context, attrs, defStyleAttr, defStyleRes) {

    val textOnScreen: CheckBox
    val subtitles: CheckBox
    val metadata: CheckBox
    val searchText: TextView

    init {
        // inflate the image_query_tools layout to this view
        LayoutInflater.from(context).inflate(R.layout.text_query_tools, toolsContainer, true)

        textOnScreen = toolsContainer.findViewById(R.id.textOnScreen)
        subtitles = toolsContainer.findViewById(R.id.subtitles)
        metadata = toolsContainer.findViewById(R.id.metadata)
        searchText = toolsContainer.findViewById(R.id.searchText)

        textOnScreen.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) queryViewModel.addTextQueryCategory(queryViewModel.currContainerID, "tos")
            else queryViewModel.removeTextQueryCategory(queryViewModel.currContainerID, "tos")
        }

        subtitles.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) queryViewModel.addTextQueryCategory(queryViewModel.currContainerID, "subtitles")
            else queryViewModel.removeTextQueryCategory(queryViewModel.currContainerID, "subtitles")
        }

        metadata.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) queryViewModel.addTextQueryCategory(queryViewModel.currContainerID, "metadata")
            else queryViewModel.removeTextQueryCategory(queryViewModel.currContainerID, "metadata")
        }

        searchText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                queryViewModel.setDataOfQueryTerm(queryViewModel.currContainerID, QueryTermType.TEXT, s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        if (wasChecked) {
            restoreState()
        } else {
            queryViewModel.addQueryTermToContainer(queryViewModel.currContainerID, QueryTermType.TEXT)
        }
    }

    private fun restoreState() {
        val categories = queryViewModel.getTextQueryCategories(queryViewModel.currContainerID)
        val copy = ArrayList<String>()
        categories?.let { copy.addAll(it) }
        copy?.forEach {
            when (it) {
                "tos" -> textOnScreen.isChecked = true
                "subtitles" -> subtitles.isChecked = true
                "metadata" -> metadata.isChecked = true
            }
        }

        val data = queryViewModel.getTextQueryData(queryViewModel.currContainerID)
        searchText.text = data
    }
}
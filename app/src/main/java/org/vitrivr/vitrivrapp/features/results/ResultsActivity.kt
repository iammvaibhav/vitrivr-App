package org.vitrivr.vitrivrapp.features.results

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import kotlinx.android.synthetic.main.results_activity.*
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.components.results.EqualSpacingItemDecoration
import org.vitrivr.vitrivrapp.data.model.enums.MediaType
import org.vitrivr.vitrivrapp.data.model.enums.MessageType
import org.vitrivr.vitrivrapp.data.model.enums.ResultViewType
import org.vitrivr.vitrivrapp.utils.px
import java.util.*

class ResultsActivity : AppCompatActivity() {

    lateinit var resultsViewModel: ResultsViewModel

    val CURRENT_RESULTS = "CURRENT_RESULTS"
    val CURRENT_RESULT_VIEW = "CURRENT_RESULT_VIEW"
    val SAVED_LAYOUT_MANAGER = "SAVED_LAYOUT_MANAGER"
    val MEDIA_TYPE_CATEGORIES = "MEDIA_TYPE_CATEGORIES"

    companion object {
        val QUERY_TYPE = "QUERY_TYPE"
    }

    val largeViewAdapter by lazy { ViewDetailsAdapter(listOf(), ResultViewType.LARGE, resultsViewModel, ::startQuery) }
    val mediumViewAdapter by lazy { ViewDetailsAdapter(listOf(), ResultViewType.MEDIUM, resultsViewModel, ::startQuery) }
    val smallViewAdapter by lazy { ViewSmallAdapter(listOf(), resultsViewModel) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.results_activity)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        resultsViewModel = ViewModelProviders.of(this).get(ResultsViewModel::class.java)

        if (resultsViewModel.isNewViewModel) {
            resultsViewModel.isNewViewModel = false

            // restore resultsViewModel state if exists
            savedInstanceState?.let {
                resultsViewModel.restoreCurrentPresenterResults()
                resultsViewModel.currResultViewType = it.getSerializable(CURRENT_RESULT_VIEW) as ResultViewType
                resultsViewModel.categoryCount = it.getSerializable(MEDIA_TYPE_CATEGORIES) as HashMap<MediaType, HashSet<String>>
            }
        }

        if (savedInstanceState == null) {
            val queryType = intent.getSerializableExtra(QUERY_TYPE) as MessageType
            val queryString = when (queryType) {
                MessageType.Q_SIM -> resultsViewModel.queryToJson()
                MessageType.Q_MLT -> intent.getStringExtra("query")
                else -> ""
            }
            viewLarge(view_large)
            startQuery(queryString)
        } else {
            //restore UI state
            queryResultsRV.adapter = getAdapter(resultsViewModel.currResultViewType)
            when (resultsViewModel.currResultViewType) {
                ResultViewType.LARGE -> viewLarge(view_large)
                ResultViewType.MEDIUM -> viewMedium(view_medium)
                ResultViewType.SMALL -> viewSmall(view_small)
            }
            queryResultsRV.layoutManager.onRestoreInstanceState(savedInstanceState.getParcelable(SAVED_LAYOUT_MANAGER))
        }
    }

    private fun startQuery(query: String) {
        progressBar.visibility = View.VISIBLE
        val queryResults = resultsViewModel.getQueryResults(query, ::failure, ::closed)

        queryResults.observe(this, Observer {
            it?.let {
                if (queryResultsRV.adapter == null) {
                    if (resultsViewModel.currResultViewType == ResultViewType.LARGE || resultsViewModel.currResultViewType == ResultViewType.MEDIUM) {
                        queryResultsRV.adapter = getAdapter(resultsViewModel.currResultViewType)
                        (queryResultsRV.adapter as ViewDetailsAdapter).swap(it)
                    } else {
                        queryResultsRV.adapter = getAdapter(ResultViewType.SMALL)
                        (queryResultsRV.adapter as ViewSmallAdapter).swap(it)
                    }
                } else {
                    if (resultsViewModel.currResultViewType == ResultViewType.LARGE || resultsViewModel.currResultViewType == ResultViewType.MEDIUM) {
                        (queryResultsRV.adapter as ViewDetailsAdapter).swap(it)
                    } else {
                        (queryResultsRV.adapter as ViewSmallAdapter).swap(it)
                    }
                }
            }
        })
    }

    private fun failure(reason: String) {
        runOnUiThread {
            Toast.makeText(this, reason, Toast.LENGTH_LONG).show()
            progressBar.visibility = View.INVISIBLE
        }
    }

    private fun closed() {
        runOnUiThread {
            progressBar.visibility = View.INVISIBLE
        }
    }

    fun viewLarge(view: View) {
        select(view as ImageView)
        for (i in 0 until queryResultsRV.itemDecorationCount) {
            queryResultsRV.removeItemDecorationAt(i)
        }
        queryResultsRV.addItemDecoration(EqualSpacingItemDecoration(8.px, EqualSpacingItemDecoration.VERTICAL))
        queryResultsRV.layoutManager = LinearLayoutManager(this)

        if (queryResultsRV.adapter != null) {
            queryResultsRV.adapter = getAdapter(ResultViewType.LARGE)
            resultsViewModel.getCurrentResults().value?.let {
                (queryResultsRV.adapter as ViewDetailsAdapter).swap(it)
            }
        }

        resultsViewModel.currResultViewType = ResultViewType.LARGE
    }

    fun viewMedium(view: View) {
        select(view as ImageView)
        for (i in 0 until queryResultsRV.itemDecorationCount) {
            queryResultsRV.removeItemDecorationAt(i)
        }
        queryResultsRV.addItemDecoration(EqualSpacingItemDecoration(8.px, EqualSpacingItemDecoration.GRID))
        queryResultsRV.layoutManager = GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false)

        if (queryResultsRV.adapter != null) {
            queryResultsRV.adapter = getAdapter(ResultViewType.MEDIUM)
            resultsViewModel.getCurrentResults().value?.let {
                (queryResultsRV.adapter as ViewDetailsAdapter).swap(it)
            }
        }
        resultsViewModel.currResultViewType = ResultViewType.MEDIUM
    }

    fun viewSmall(view: View) {
        select(view as ImageView)
        for (i in 0 until queryResultsRV.itemDecorationCount) {
            queryResultsRV.removeItemDecorationAt(i)
        }
        queryResultsRV.addItemDecoration(EqualSpacingItemDecoration(8.px, EqualSpacingItemDecoration.GRID))
        queryResultsRV.layoutManager = GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false)

        if (queryResultsRV.adapter != null) {
            queryResultsRV.adapter = getAdapter(ResultViewType.SMALL)
            resultsViewModel.getCurrentResults().value?.let {
                (queryResultsRV.adapter as ViewSmallAdapter).swap(it)
            }
        }
        resultsViewModel.currResultViewType = ResultViewType.SMALL
    }

    private fun select(view: ImageView) {
        view_large.setColorFilter(Color.WHITE)
        view_medium.setColorFilter(Color.WHITE)
        view_small.setColorFilter(Color.WHITE)
        view.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimaryDark))
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState?.let { bundle ->
            resultsViewModel.saveCurrentPresenterResults()
            bundle.putParcelable(SAVED_LAYOUT_MANAGER, queryResultsRV.layoutManager.onSaveInstanceState())
            bundle.putSerializable(CURRENT_RESULT_VIEW, resultsViewModel.currResultViewType)
            bundle.putSerializable(MEDIA_TYPE_CATEGORIES, resultsViewModel.categoryCount)
        }
    }

    private fun getAdapter(resultViewType: ResultViewType) = when (resultViewType) {
        ResultViewType.LARGE -> largeViewAdapter
        ResultViewType.MEDIUM -> mediumViewAdapter
        ResultViewType.SMALL -> smallViewAdapter
    }

}
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
import org.vitrivr.vitrivrapp.data.model.enums.ResultViewType
import org.vitrivr.vitrivrapp.utils.px

class ResultsActivity : AppCompatActivity() {

    lateinit var resultsViewModel: ResultsViewModel
    var currResultViewType = ResultViewType.LARGE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.results_activity)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val queryString = intent.getStringExtra("query")
        if (queryString == null) {
            Toast.makeText(this, "Error! No Query Found.", Toast.LENGTH_SHORT).show()
            finish()
        }

        resultsViewModel = ViewModelProviders.of(this).get(ResultsViewModel::class.java)
        viewLarge(view_large)
        startQuery(queryString)
    }

    private fun startQuery(query: String) {
        progressBar.visibility = View.VISIBLE
        val queryResults = resultsViewModel.getQueryResults(query, ::failure, ::closed)

        queryResults.observe(this, Observer {
            it?.let {
                if (queryResultsRV.adapter == null) {
                    if (currResultViewType == ResultViewType.LARGE || currResultViewType == ResultViewType.MEDIUM) {
                        queryResultsRV.adapter = ViewDetailsAdapter(it, currResultViewType)
                    } else {
                        queryResultsRV.adapter = ViewSmallAdapter(it)
                    }
                } else {
                    /*val diffResult = DiffUtil.calculateDiff(GradualQueryResultsCallback((queryResultsRV.adapter as ViewDetailsAdapter).items, it))
                    diffResult.dispatchUpdatesTo(queryResultsRV.adapter)*/
                    queryResultsRV.adapter.notifyDataSetChanged()
                    //TODO(Use diffUtil to update items instead of notifyDataSetChange)
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
            if (currResultViewType == ResultViewType.MEDIUM || currResultViewType == ResultViewType.LARGE)
                queryResultsRV.adapter = ViewDetailsAdapter((queryResultsRV.adapter as ViewDetailsAdapter).items, ResultViewType.LARGE)
            else queryResultsRV.adapter = ViewDetailsAdapter((queryResultsRV.adapter as ViewSmallAdapter).items, ResultViewType.LARGE)
        }
        currResultViewType = ResultViewType.LARGE
    }

    fun viewMedium(view: View) {
        select(view as ImageView)
        for (i in 0 until queryResultsRV.itemDecorationCount) {
            queryResultsRV.removeItemDecorationAt(i)
        }
        queryResultsRV.addItemDecoration(EqualSpacingItemDecoration(8.px, EqualSpacingItemDecoration.GRID))
        queryResultsRV.layoutManager = GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false)
        if (queryResultsRV.adapter != null) {
            if (currResultViewType == ResultViewType.MEDIUM || currResultViewType == ResultViewType.LARGE)
                queryResultsRV.adapter = ViewDetailsAdapter((queryResultsRV.adapter as ViewDetailsAdapter).items, ResultViewType.MEDIUM)
            else queryResultsRV.adapter = ViewDetailsAdapter((queryResultsRV.adapter as ViewSmallAdapter).items, ResultViewType.MEDIUM)
        }
        currResultViewType = ResultViewType.MEDIUM
    }

    fun viewSmall(view: View) {
        select(view as ImageView)
        for (i in 0 until queryResultsRV.itemDecorationCount) {
            queryResultsRV.removeItemDecorationAt(i)
        }
        queryResultsRV.addItemDecoration(EqualSpacingItemDecoration(8.px, EqualSpacingItemDecoration.GRID))
        queryResultsRV.layoutManager = GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false)
        if (queryResultsRV.adapter != null) {
            if (currResultViewType == ResultViewType.MEDIUM || currResultViewType == ResultViewType.LARGE)
                queryResultsRV.adapter = ViewSmallAdapter((queryResultsRV.adapter as ViewDetailsAdapter).items)
            else queryResultsRV.adapter = ViewSmallAdapter((queryResultsRV.adapter as ViewSmallAdapter).items)
        }
        currResultViewType = ResultViewType.SMALL
    }

    private fun select(view: ImageView) {
        view_large.setColorFilter(Color.WHITE)
        view_medium.setColorFilter(Color.WHITE)
        view_small.setColorFilter(Color.WHITE)
        view.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimaryDark))
    }
}
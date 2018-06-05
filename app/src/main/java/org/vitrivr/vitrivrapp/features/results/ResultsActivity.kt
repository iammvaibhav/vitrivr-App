package org.vitrivr.vitrivrapp.features.results

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import kotlinx.android.synthetic.main.results_activity.*
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.components.results.EqualSpacingItemDecoration
import org.vitrivr.vitrivrapp.utils.px

class ResultsActivity : AppCompatActivity() {

    lateinit var resultsViewModel: ResultsViewModel

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
        resultsViewModel.query = queryString
        Log.e("rawQuery", resultsViewModel.query)

        queryResultsRV.addItemDecoration(EqualSpacingItemDecoration(8.px, EqualSpacingItemDecoration.VERTICAL))
        viewLarge(view_large)

        val queryResults = resultsViewModel.getQueryResults(::failure, ::closed)

        queryResults.observe(this, Observer {
            it?.forEach {
                Log.e("result", it.toString())
            }
            it?.let {
                queryResultsRV.adapter = ViewLargeAdapter(it, resultsViewModel.getDirectoryPath())
            }
        })

    }

    private fun failure(reason: String) {
        runOnUiThread {
            Toast.makeText(this, reason, Toast.LENGTH_LONG).show()
        }
    }

    private fun closed() {
        runOnUiThread {
            Toast.makeText(this, "Closed", Toast.LENGTH_LONG).show()
        }
    }

    fun viewLarge(view: View) {
        select(view as ImageView)
        queryResultsRV.layoutManager = LinearLayoutManager(this)
    }

    fun viewMedium(view: View) {
        select(view as ImageView)
    }

    fun viewSmall(view: View) {
        select(view as ImageView)
    }

    private fun select(view: ImageView) {
        view_large.setColorFilter(Color.WHITE)
        view_medium.setColorFilter(Color.WHITE)
        view_small.setColorFilter(Color.WHITE)
        view.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimaryDark))
    }
}
package org.vitrivr.vitrivrapp.features.results

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import org.vitrivr.vitrivrapp.R

class ResultsActivity : AppCompatActivity() {

    lateinit var resultsViewModel: ResultsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.results_activity)

        val queryString = intent.getStringExtra("query")
        if (queryString == null) {
            Toast.makeText(this, "Error! No Query Found.", Toast.LENGTH_SHORT).show()
            finish()
        }

        resultsViewModel = ViewModelProviders.of(this).get(ResultsViewModel::class.java)
        resultsViewModel.query = queryString

        val queryResults = resultsViewModel.getQueryResults(::failure, ::closed)
        queryResults.observe(this, Observer {
            it?.forEach {
                Log.e("result", it.toString())
            }
        })
    }

    private fun failure(reason: String) {
        runOnUiThread {
            Toast.makeText(this, reason, Toast.LENGTH_LONG).show()
        }
    }

    private fun closed(code: Int) {
        runOnUiThread {
            Toast.makeText(this, code.toString(), Toast.LENGTH_LONG).show()
        }
    }
}
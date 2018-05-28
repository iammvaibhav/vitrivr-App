package org.vitrivr.vitrivrapp.features.query

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.query_activity.*
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.features.settings.SettingsActivity
import org.vitrivr.vitrivrapp.utils.px

class QueryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.query_activity)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        if (queryContainers.childCount == 0) {
            addQueryContainer(null)
        }
    }

    fun addQueryContainer(view: View?) {
        val newContainer = QueryContainer(this)
        val layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(10.px, 10.px, 10.px, 10.px)
        newContainer.layoutParams = layoutParams
        newContainer.addDeleteQueryContainerListener {
            if (queryContainers.childCount > 1)
                queryContainers.removeView(newContainer)
        }
        newContainer.addQueryTermToggleListener { queryTerm, checked -> Log.e("sdf", "dsf") }
        queryContainers.addView(newContainer)
    }

    fun openSettings(view: View) {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    fun clearAll(view: View) {
        queryContainers.removeAllViews()
        addQueryContainer(null)
    }

}
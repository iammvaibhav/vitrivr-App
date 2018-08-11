package org.vitrivr.vitrivrapp.features.results

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.results_activity.*
import kotlinx.android.synthetic.main.results_query_refinement.*
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.components.results.EqualSpacingItemDecoration
import org.vitrivr.vitrivrapp.data.model.enums.MediaType
import org.vitrivr.vitrivrapp.data.model.enums.MessageType
import org.vitrivr.vitrivrapp.data.model.enums.ResultViewType
import org.vitrivr.vitrivrapp.features.settings.SettingsActivity
import org.vitrivr.vitrivrapp.utils.px
import org.vitrivr.vitrivrapp.utils.showToast

@Suppress("UNCHECKED_CAST", "NestedLambdaShadowedImplicitParameter", "UNUSED_PARAMETER")
class ResultsActivity : AppCompatActivity() {

    lateinit var resultsViewModel: ResultsViewModel

    companion object {
        const val SAVED_LAYOUT_MANAGER = "SAVED_LAYOUT_MANAGER"
        const val QUERY_TYPE = "QUERY_TYPE"
        const val PRESENTER_OBJECT = "PRESENTER_OBJECT"
        const val CATEGORY_INFO = "CATEGORY_INFO"
    }

    /**
     * All types of adapters are lazy initialized. To feed the results, SwapAdapter#swap method is used.
     */
    private val largeViewAdapter by lazy { ViewDetailsAdapter(ResultViewType.LARGE, resultsViewModel, ::startQuery) }
    private val mediumViewAdapter by lazy { ViewDetailsAdapter(ResultViewType.MEDIUM, resultsViewModel, ::startQuery) }
    private val smallViewAdapter by lazy { ViewSmallAdapter(resultsViewModel) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.results_activity)

        /**
         * setup the toolbar
         */
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        resultsViewModel = ViewModelProviders.of(this).get(ResultsViewModel::class.java)

        /**
         * obtain the QueryViewModel object for this activity from ViewModel Provider
         */
        if (resultsViewModel.isNewViewModel) {

            /**
             * Checks if queryViewModel is newly constructed. If it is, un-mark it as new, and restore
             * the queryViewModel state from the savedInstanceState bundle
             *
             * This will help to use ViewModel between orientation changes and restore ViewModel in case
             * of activity kill in low memory situations
             */
            resultsViewModel.isNewViewModel = false

            /**
             * restore resultsViewModel state if exists
             */
            savedInstanceState?.let {
                resultsViewModel.restoreCurrentViewModelState()
            }
        }

        /**
         * initializing the weight adjustment drawer
         */
        weightAdjustmentRecyclerView.layoutManager = LinearLayoutManager(this)
        drawer.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerStateChanged(newState: Int) {
                if (newState == DrawerLayout.STATE_SETTLING && !drawer.isDrawerOpen(Gravity.END)) {

                    /**
                     * Drawer started opening
                     */
                    weightAdjustmentRecyclerView.adapter = WeightAdjustmentAdapter(resultsViewModel.categoryWeight)

                    /**
                     * set the media filter checkbox
                     */
                    imageFilter.isChecked = resultsViewModel.mediaTypeVisibility[MediaType.IMAGE] ?: false
                    videoFilter.isChecked = resultsViewModel.mediaTypeVisibility[MediaType.VIDEO] ?: false
                    audioFilter.isChecked = resultsViewModel.mediaTypeVisibility[MediaType.AUDIO] ?: false
                    model3dFilter.isChecked = resultsViewModel.mediaTypeVisibility[MediaType.MODEL3D] ?: false

                    /**
                     * if media type is unchecked, set the visibility to GONE
                     */
                    for (i in 0 until mediaTypeFilters.childCount) {
                        val child = mediaTypeFilters.getChildAt(i) as LinearLayout
                        val childMediaType = MediaType.valueOf((child.getChildAt(0) as TextView).text.toString())
                        child.visibility = View.GONE
                        if (childMediaType in resultsViewModel.mediaTypeVisibility) {
                            child.visibility = View.VISIBLE
                        }
                    }
                }
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerClosed(drawerView: View) {}

            override fun onDrawerOpened(drawerView: View) {}
        })

        /**
         * set media filters listeners
         */
        imageFilter.setOnCheckedChangeListener { _, isChecked -> resultsViewModel.mediaTypeVisibility[MediaType.IMAGE] = isChecked }
        videoFilter.setOnCheckedChangeListener { _, isChecked -> resultsViewModel.mediaTypeVisibility[MediaType.VIDEO] = isChecked }
        audioFilter.setOnCheckedChangeListener { _, isChecked -> resultsViewModel.mediaTypeVisibility[MediaType.AUDIO] = isChecked }
        model3dFilter.setOnCheckedChangeListener { _, isChecked -> resultsViewModel.mediaTypeVisibility[MediaType.MODEL3D] = isChecked }


        applyRefinement.setOnClickListener {
            resultsViewModel.applyRefinements()
            drawer.closeDrawer(Gravity.END)
        }

        clearRefinements.setOnClickListener {
            resultsViewModel.mediaTypeVisibility.forEach { resultsViewModel.mediaTypeVisibility[it.key] = true }
            resultsViewModel.categoryWeight.forEach { resultsViewModel.categoryWeight[it.key] = 1.0 }
            resultsViewModel.applyRefinements()
            drawer.closeDrawer(Gravity.END)
        }

        if (savedInstanceState == null) {
            /**
             * newly created activity
             */
            val queryType = intent.getSerializableExtra(QUERY_TYPE) as MessageType

            /**
             * based on the query type, obtain the query string and start the query
             */
            val queryString = when (queryType) {
                MessageType.Q_SIM -> resultsViewModel.queryToJson()
                MessageType.Q_MLT -> intent.getStringExtra("query")
                else -> ""
            }

            /**
             * default view is large view
             */
            viewLarge(view_large)

            startQuery(queryString)
        } else {
            /**
             * restore UI state, get the preview result type adapter and set the corresponding view type
             */
            queryResultsRV.adapter = getAdapter(resultsViewModel.currResultViewType)
            when (resultsViewModel.currResultViewType) {
                ResultViewType.LARGE -> viewLarge(view_large)
                ResultViewType.MEDIUM -> viewMedium(view_medium)
                ResultViewType.SMALL -> viewSmall(view_small)
            }
            queryResultsRV.layoutManager.onRestoreInstanceState(savedInstanceState.getParcelable(SAVED_LAYOUT_MANAGER))
        }
    }

    /**
     * start the query from the given query string
     * @param query query String representing a valid JSON query
     */
    private fun startQuery(query: String) {
        progressBar.visibility = View.VISIBLE
        val queryResults = resultsViewModel.getQueryResults(query, ::failure, ::closed)

        /**
         * queryResults is a LiveData object which is null if API Settings are not configured
         */
        if (queryResults == null) {
            "Cineast API Settings are not configured. Please configure it first before querying".showToast(this)
            startActivity(Intent(this, SettingsActivity::class.java))
        } else {

            /**
             * start observing for results
             */
            queryResults.observe(this, Observer {
                it?.let {
                    /**
                     * if adapter is null, then set the adapter according to the current view type
                     */
                    if (queryResultsRV.adapter == null) {
                        if (resultsViewModel.currResultViewType == ResultViewType.LARGE || resultsViewModel.currResultViewType == ResultViewType.MEDIUM) {
                            queryResultsRV.adapter = getAdapter(resultsViewModel.currResultViewType)
                        } else {
                            queryResultsRV.adapter = getAdapter(ResultViewType.SMALL)
                        }
                    }
                    /**
                     * swap the results
                     */
                    (queryResultsRV.adapter as SwapAdapter).swap(it)
                }
            })
        }
    }

    /**
     * this method is invoked when a failure occurs while querying
     * @param reason reason why the failure occurred
     */
    private fun failure(reason: String) {
        runOnUiThread {
            Toast.makeText(this, reason, Toast.LENGTH_LONG).show()
            progressBar.visibility = View.INVISIBLE
        }
    }

    /**
     * this method is invoked when the query is completed and underlying websocket is closed
     */
    private fun closed() {
        runOnUiThread {
            progressBar.visibility = View.INVISIBLE
        }
    }

    /**
     * invoked when user presses view large button on toolbar. Switches the result view to show items in large details.
     */
    fun viewLarge(view: View) {
        /**
         * highlight the view as selected
         */
        select(view as ImageView)

        /**
         * remove all item decorations from query results recycler view and add according to this view type
         */
        for (i in 0 until queryResultsRV.itemDecorationCount) {
            queryResultsRV.removeItemDecorationAt(i)
        }
        queryResultsRV.addItemDecoration(EqualSpacingItemDecoration(8.px, EqualSpacingItemDecoration.VERTICAL))

        /**
         * set the layout manager & if the current adapter is not null, then set the adapter for large details and
         * show the current result items.
         */
        queryResultsRV.layoutManager = LinearLayoutManager(this)
        if (queryResultsRV.adapter != null) {
            queryResultsRV.adapter = getAdapter(ResultViewType.LARGE)
            resultsViewModel.getCurrentResults().observe(this, Observer {
                it?.let { (queryResultsRV.adapter as SwapAdapter).swap(it) }
            })
        }

        /**
         * set the current result type to LARGE
         */
        resultsViewModel.currResultViewType = ResultViewType.LARGE
    }

    /**
     * invoked when user presses view medium button on toolbar. Switches the result view to show items in medium details.
     */
    fun viewMedium(view: View) {
        /**
         * highlight the view as selected
         */
        select(view as ImageView)

        /**
         * remove all item decorations from query results recycler view and add according to this view type
         */
        for (i in 0 until queryResultsRV.itemDecorationCount) {
            queryResultsRV.removeItemDecorationAt(i)
        }
        queryResultsRV.addItemDecoration(EqualSpacingItemDecoration(8.px, EqualSpacingItemDecoration.GRID))

        /**
         * set the layout manager & if the current adapter is not null, then set the adapter for medium details and
         * show the current result items.
         */
        queryResultsRV.layoutManager = GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false)
        if (queryResultsRV.adapter != null) {
            queryResultsRV.adapter = getAdapter(ResultViewType.MEDIUM)
            resultsViewModel.getCurrentResults().observe(this, Observer {
                it?.let { (queryResultsRV.adapter as SwapAdapter).swap(it) }
            })
        }

        /**
         * set the current result type to MEDIUM
         */
        resultsViewModel.currResultViewType = ResultViewType.MEDIUM
    }

    /**
     * invoked when user presses small button on toolbar. Switches the result view to show items preview without details.
     */
    fun viewSmall(view: View) {
        /**
         * highlight the view as selected
         */
        select(view as ImageView)

        /**
         * remove all item decorations from query results recycler view and add according to this view type
         */
        for (i in 0 until queryResultsRV.itemDecorationCount) {
            queryResultsRV.removeItemDecorationAt(i)
        }
        queryResultsRV.addItemDecoration(EqualSpacingItemDecoration(8.px, EqualSpacingItemDecoration.GRID))

        /**
         * set the layout manager & if the current adapter is not null, then set the adapter for preview without details and
         * show the current result items.
         */
        queryResultsRV.layoutManager = GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false)
        if (queryResultsRV.adapter != null) {
            queryResultsRV.adapter = getAdapter(ResultViewType.SMALL)
            resultsViewModel.getCurrentResults().observe(this, Observer {
                it?.let { (queryResultsRV.adapter as SwapAdapter).swap(it) }
            })
        }

        /**
         * set the current result type to MEDIUM
         */
        resultsViewModel.currResultViewType = ResultViewType.SMALL
    }

    /**
     * invoked when user presses the query refinement drawer icon in toolbar
     */
    fun queryRefinement(view: View) {
        if (drawer.isDrawerOpen(Gravity.END)) {
            drawer.closeDrawer(Gravity.END)
        } else {
            drawer.openDrawer(Gravity.END)
        }
    }

    /**
     * visually highlight the view type ImageView to show it is selected
     * @param view view type ImageView to highlight
     */
    private fun select(view: ImageView) {
        view_large.setColorFilter(Color.WHITE)
        view_medium.setColorFilter(Color.WHITE)
        view_small.setColorFilter(Color.WHITE)
        view.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimaryDark))
    }

    /**
     * returns the adapter for the ResultViewType
     * @param resultViewType view type to get adapter for
     * @return RecyclerView.Adapter
     */
    private fun getAdapter(resultViewType: ResultViewType): RecyclerView.Adapter<*> {
        return when (resultViewType) {
            ResultViewType.LARGE -> largeViewAdapter
            ResultViewType.MEDIUM -> mediumViewAdapter
            ResultViewType.SMALL -> smallViewAdapter
        }
    }

    override fun onStop() {
        super.onStop()

        /**
         * If the activity is finishing
         */
        if (isFinishing) {
            /**
             * remove resultsViewModel state if exists
             */
            resultsViewModel.removeViewModelState()
        }

        /**
         * If activity is not changing configuration and is not finishing, save the current view model state
         */
        if (!isChangingConfigurations && !isFinishing) {
            resultsViewModel.saveCurrentViewModelState()
        }
    }

    /**
     * save layout manager state
     */
    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState?.putParcelable(SAVED_LAYOUT_MANAGER, queryResultsRV.layoutManager.onSaveInstanceState())
    }
}
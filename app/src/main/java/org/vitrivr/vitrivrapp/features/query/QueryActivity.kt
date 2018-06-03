package org.vitrivr.vitrivrapp.features.query

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.widget.NestedScrollView
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import kotlinx.android.synthetic.main.query_activity.*
import kotlinx.android.synthetic.main.query_detail_bottom_sheet.*
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.components.drawing.DrawingActivity
import org.vitrivr.vitrivrapp.components.drawing.DrawingActivity.Companion.IMAGE_PATH
import org.vitrivr.vitrivrapp.data.model.*
import org.vitrivr.vitrivrapp.features.query.QueryToggles.QueryTerm
import org.vitrivr.vitrivrapp.features.settings.SettingsActivity
import org.vitrivr.vitrivrapp.utils.px
import java.io.ByteArrayOutputStream
import java.io.File

class QueryActivity : AppCompatActivity() {

    val DRAWING_RESULT = 1
    val QUERY_OBJECT = "QUERY_OBJECT"
    val CURR_CONTAINER_ID = "CURR_CONTAINER_ID"
    val CURR_TERM_TYPE = "CURR_TERM_TYPE"

    lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    lateinit var queryViewModel: QueryViewModel
    lateinit var bottomSheetToggles: QueryToggles

    val enableTermListener = { _: CompoundButton, isChecked: Boolean ->
        if (!isChecked) {
            toolsContainer.removeAllViews()
            queryViewModel.removeQueryTermFromContainer(queryViewModel.currContainerID, queryViewModel.currTermType)
            getQueryContainerWithId(queryViewModel.currContainerID)?.setChecked(queryViewModel.currTermType, false)
            freeResources(queryViewModel.currTermType, queryViewModel.currContainerID)
            toolTitle.text = ""
            bottomSheetToggles.setChecked(queryViewModel.currTermType, false)
        } else {
            getQueryContainerWithId(queryViewModel.currContainerID)?.setChecked(queryViewModel.currTermType, true)
            prepareBottomSheet(queryViewModel.currTermType, false, queryViewModel.currContainerID)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.query_activity)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        queryViewModel = ViewModelProviders.of(this).get(QueryViewModel::class.java)

        if (queryViewModel.isNewViewModel) {
            queryViewModel.isNewViewModel = false

            // restore queryViewModel state if exists
            savedInstanceState?.let {
                val currContainerId = it.getLong(CURR_CONTAINER_ID, -1L)
                if (currContainerId != -1L) {
                    queryViewModel.currContainerID = currContainerId
                    queryViewModel.query = it.getParcelable(QUERY_OBJECT) as QueryModel
                    queryViewModel.currTermType = it.getSerializable(CURR_TERM_TYPE) as QueryTerm
                }
            }
        }

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.skipCollapsed = true
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.peekHeight = 0

        if (queryViewModel.query.containers.isEmpty()) {
            // fresh query
            addQueryContainer(null)
        } else {
            // restore UI state from queryViewModel
            for (container in queryViewModel.query.containers) {
                val newContainer = getNewContainerWithId(container.id)
                queryContainers.addView(newContainer)
                restoreContainerUIState(newContainer, container.terms, container.description)
            }
        }

        bottomSheetToggles = bottomSheet.findViewById(R.id.queryToggles)

        bottomSheetToggles.addQueryTermClickListener { queryTerm, wasChecked ->
            /*prepareBottomSheet(queryTerm, wasChecked, queryViewModel.currContainerID)*/
            Toast.makeText(this, "Not Implemented", Toast.LENGTH_SHORT).show()
            bottomSheetToggles.setChecked(queryTerm, false)
        }
    }

    fun addQueryContainer(view: View?) {
        val containerId = queryViewModel.addContainer()
        queryContainers.addView(getNewContainerWithId(containerId))
    }

    fun getNewContainerWithId(containerId: Long): QueryContainer {
        val newContainer = QueryContainer(this)
        newContainer.setTag(R.id.container_id, containerId)

        val layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(10.px, 10.px, 10.px, 10.px)
        newContainer.layoutParams = layoutParams

        newContainer.addDeleteQueryContainerListener {
            if (queryContainers.childCount > 1) {
                queryContainers.removeView(newContainer)
                queryViewModel.removeContainer(containerId)
            }
        }

        newContainer.addQueryTermClickListener { queryTerm, wasChecked ->
            queryViewModel.currContainerID = containerId
            queryViewModel.currTermType = queryTerm
            prepareBottomSheet(queryTerm, wasChecked, containerId)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        newContainer.addQueryDescriptionChangeListener {
            queryViewModel.setQueryDescriptionOfContainer(containerId, it)
        }

        return newContainer
    }

    fun restoreContainerUIState(queryContainer: QueryContainer, queryTerms: List<QueryTermModel>, description: String) {
        queryContainer.setQueryDescription(description)
        for (term in queryTerms) {
            queryContainer.setChecked(term.type, true)
        }
    }

    fun openSettings(view: View) {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    fun clearAll(view: View) {
        queryContainers.removeAllViews()
        addQueryContainer(null)
    }

    fun openDrawingActivity(view: View) {
        val intent = Intent(this, DrawingActivity::class.java)
        intent.putExtra("containerID", queryViewModel.currContainerID)
        startActivityForResult(intent, DRAWING_RESULT)
    }

    fun collapseBottomSheet(view: View) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    fun search(view: View) {
        val result = queryViewModel.search({ Log.e("Failure", it) }, { Log.e("Closed", "$it") })
        result.observe(this, Observer {
            val s = when (it?.messageType) {
                MessageType.QR_START -> it as QueryResultStartModel?
                MessageType.QR_END -> it as QueryResultEndModel?
                MessageType.QR_SEGMENT -> it as QueryResultSegmentModel?
                MessageType.QR_OBJECT -> it as QueryResultObjectModel?
                MessageType.QR_SIMILARITY -> it as QueryResultSimilarityModel?
                else -> it
            }
            Log.e("object received : ", s.toString())
        })
    }

    fun getQueryContainerWithId(containerId: Long): QueryContainer? {
        for (i in 0 until queryContainers.childCount) {
            if (queryContainers.getChildAt(i).getTag(R.id.container_id) == containerId)
                return queryContainers.getChildAt(i) as QueryContainer
        }
        return null
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        // save state of queryViewModel
        outState?.let {
            it.putParcelable(QUERY_OBJECT, queryViewModel.query)
            it.putLong(CURR_CONTAINER_ID, queryViewModel.currContainerID)
            it.putSerializable(CURR_TERM_TYPE, queryViewModel.currTermType)
        }

    }

    fun prepareBottomSheet(type: QueryTerm, wasChecked: Boolean, containerId: Long) {
        when (type) {
            QueryTerm.IMAGE -> {
                toolTitle.text = "Image Query"
                enableTerm.setOnCheckedChangeListener(null)
                enableTerm.isChecked = true
                bottomSheetToggles.setChecked(QueryTerm.IMAGE, true)
                enableTerm.setOnCheckedChangeListener(enableTermListener)

                toolsContainer.removeAllViews()
                LayoutInflater.from(this).inflate(R.layout.image_query_tools, toolsContainer, true)
                val drawImageBalance = toolsContainer.findViewById<SeekBar>(R.id.drawImageBalance)
                drawImageBalance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        queryViewModel.setBalance(containerId, QueryTerm.IMAGE, progress)
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })

                if (wasChecked) {
                    // restore state
                    drawImageBalance.progress = queryViewModel.getBalance(containerId, QueryTerm.IMAGE)
                    val image = BitmapFactory.decodeFile(File(filesDir, "imageQuery_image_${queryViewModel.currContainerID}.png").absolutePath)
                    toolsContainer.findViewById<ImageView>(R.id.imagePreview).setImageBitmap(image)
                } else {
                    queryViewModel.addQueryTermToContainer(containerId, QueryTerm.IMAGE)
                }
            }
        //TODO("Other types")
        }
    }

    fun freeResources(type: QueryTerm, containerID: Long) {
        when (type) {
            QueryTerm.IMAGE -> {
                val preview = File(filesDir, "imageQuery_image_$containerID.png")
                val orig = File(filesDir, "imageQuery_image_orig_$containerID.png")
                if (preview.exists()) preview.delete()
                if (orig.exists()) orig.delete()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            DRAWING_RESULT -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val image = BitmapFactory.decodeFile(data.getStringExtra(IMAGE_PATH))
                    val outputStream = ByteArrayOutputStream()
                    image.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
                    queryViewModel.setDataOfQueryTerm(queryViewModel.currContainerID, QueryTerm.IMAGE, base64String)
                    getQueryContainerWithId(queryViewModel.currContainerID)?.performClick(QueryTerm.IMAGE)
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    getQueryContainerWithId(queryViewModel.currContainerID)?.performClick(QueryTerm.IMAGE)
                }
            }
        }
    }
}
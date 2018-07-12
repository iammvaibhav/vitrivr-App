package org.vitrivr.vitrivrapp.features.query

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.content.ContextCompat
import android.support.v4.widget.NestedScrollView
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CompoundButton
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import droidninja.filepicker.FilePickerConst
import kotlinx.android.synthetic.main.audio_query_tools.*
import kotlinx.android.synthetic.main.query_activity.*
import kotlinx.android.synthetic.main.query_detail_bottom_sheet.*
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.data.model.enums.MessageType
import org.vitrivr.vitrivrapp.data.model.enums.QueryTermType
import org.vitrivr.vitrivrapp.data.model.query.QueryContainerModel
import org.vitrivr.vitrivrapp.data.model.query.QueryTermModel
import org.vitrivr.vitrivrapp.features.query.tools.*
import org.vitrivr.vitrivrapp.features.results.ResultsActivity
import org.vitrivr.vitrivrapp.features.results.ResultsActivity.Companion.QUERY_TYPE
import org.vitrivr.vitrivrapp.features.settings.SettingsActivity
import org.vitrivr.vitrivrapp.utils.px
import java.io.File

const val DRAWING_RESULT = 1
const val RECORD_AUDIO_RESULT = 2
const val LOAD_AUDIO_RESULT = 3
const val MODEL_CHOOSER_RESULT = 4
const val MODEL_DRAWING_RESULT = 5
const val MOTION_DRAW_RESULT = 6

const val RECORD_REQUEST_CODE = 1
const val LOAD_AUDIO_REQUEST_CODE = 2
const val MODEL_CHOOSER_REQUEST_CODE = 3
const val CURRENT_LOCATION_REQUEST_CODE = 4

class QueryActivity : AppCompatActivity() {

    private val CURR_CONTAINER_ID = "CURR_CONTAINER_ID"
    private val CURR_TERM_TYPE = "CURR_TERM_TYPE"

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    private lateinit var queryViewModel: QueryViewModel
    private lateinit var bottomSheetToggles: QueryToggles
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var imageQueryTools: ImageQueryTools? = null
    private var audioQueryTools: AudioQueryTools? = null
    private var model3DQueryTools: Model3DQueryTools? = null
    private var motionQueryTools: MotionQueryTools? = null
    private var textQueryTools: TextQueryTools? = null
    private var locationQueryTools: LocationQueryTools? = null

    /**
     * This is a listener for switch in the bottom sheet.
     * When unchecked, it removes the tools for the current term type and frees the resources used by it
     * When checked, it prepares the bottom sheet for the current term type
     */
    private val enableTermListener = { _: CompoundButton, isChecked: Boolean ->
        if (!isChecked) {

            // visual changes
            toolsContainer.removeAllViews()
            toolTitle.text = ""
            bottomSheetToggles.setChecked(queryViewModel.currTermType, false)
            getQueryContainerWithId(queryViewModel.currContainerID)?.setChecked(queryViewModel.currTermType, false)

            // remove the data associated with this container's current query term from the QueryModel object
            queryViewModel.removeQueryTermFromContainer(queryViewModel.currContainerID, queryViewModel.currTermType)

            // free the resources used by this container's current query term
            freeResources(queryViewModel.currTermType, queryViewModel.currContainerID)

            // stop audio in case of term type AUDIO
            if (audioQueryTools != null) {
                audioQueryTools?.stopPlayback()
            }

        } else {

            // visual changes
            getQueryContainerWithId(queryViewModel.currContainerID)?.setChecked(queryViewModel.currTermType, true)

            // prepare the bottom sheet
            prepareBottomSheet(queryViewModel.currTermType, false, queryViewModel.currContainerID)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.query_activity)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // obtain the QueryViewModel object for this activity from ViewModel Provider
        queryViewModel = ViewModelProviders.of(this).get(QueryViewModel::class.java)

        if (queryViewModel.isNewViewModel) {
            queryViewModel.isNewViewModel = false

            // restore queryViewModel state if exists
            savedInstanceState?.let {
                queryViewModel.restoreQueryObject()
                val currContainerId = it.getLong(CURR_CONTAINER_ID, -1L)
                if (currContainerId != -1L) {
                    queryViewModel.currContainerID = currContainerId
                    queryViewModel.currTermType = it.getSerializable(CURR_TERM_TYPE) as QueryTermType
                }
            }
        }

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.skipCollapsed = true
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN)
                    audioQueryTools?.stopPlayback()
            }
        })

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
            //stop audio playback
            audioQueryTools?.stopPlayback()

            queryViewModel.currTermType = queryTerm
            prepareBottomSheet(queryTerm, wasChecked, queryViewModel.currContainerID)
            getQueryContainerWithId(queryViewModel.currContainerID)?.setChecked(queryTerm, true)
        }
    }

    fun addQueryContainer(view: View?) {
        val containerId = queryViewModel.addContainer()
        queryContainers.addView(getNewContainerWithId(containerId))
    }

    private fun getNewContainerWithId(containerId: Long): QueryContainer {
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

                freeResources(QueryTermType.IMAGE, containerId)
                freeResources(QueryTermType.AUDIO, containerId)
                freeResources(QueryTermType.MODEL3D, containerId)
                freeResources(QueryTermType.MOTION, containerId)
            }
        }

        newContainer.addQueryTermClickListener { queryTerm, wasChecked ->
            queryViewModel.currContainerID = containerId
            queryViewModel.currTermType = queryTerm
            prepareBottomSheet(queryTerm, wasChecked, containerId)

            var container: QueryContainerModel? = null
            for (c in queryViewModel.query.containers) {
                if (c.id == queryViewModel.currContainerID) container = c
            }

            container?.let {
                for (term in container.terms) {
                    bottomSheetToggles.setChecked(term.type, true)
                }
            }

            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        newContainer.addQueryDescriptionChangeListener {
            queryViewModel.setQueryDescriptionOfContainer(containerId, it)
        }

        return newContainer
    }

    fun restoreContainerUIState(queryContainer: QueryContainer, queryTerms: List<QueryTermModel>, description: String?) {
        description?.let { queryContainer.setQueryDescription(description) }
        for (term in queryTerms) {
            queryContainer.setChecked(term.type, true)
        }
    }

    fun openSettings(view: View) {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    fun clearAll(view: View) {
        for (container in queryViewModel.query.containers) {
            val containerId = container.id
            freeResources(QueryTermType.IMAGE, containerId)
            freeResources(QueryTermType.AUDIO, containerId)
            freeResources(QueryTermType.MODEL3D, containerId)
            freeResources(QueryTermType.MOTION, containerId)
        }
        queryContainers.removeAllViews()
        queryViewModel.query.containers.clear()
        addQueryContainer(null)
        for (term in QueryTermType.values())
            bottomSheetToggles.setChecked(term, false)
    }

    fun collapseBottomSheet(view: View) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    fun search(view: View) {
        val intent = Intent(this, ResultsActivity::class.java)
        intent.putExtra(QUERY_TYPE, MessageType.Q_SIM)
        queryViewModel.saveQueryObject()
        startActivity(intent)
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
        queryViewModel.saveQueryObject()
        // save state of queryViewModel
        outState?.let {
            it.putLong(CURR_CONTAINER_ID, queryViewModel.currContainerID)
            it.putSerializable(CURR_TERM_TYPE, queryViewModel.currTermType)
        }

    }

    private fun prepareBottomSheet(type: QueryTermType, wasChecked: Boolean, containerId: Long) {
        setupTermSwitch(type)
        toolsContainer.removeAllViews()

        when (type) {
            QueryTermType.IMAGE -> {
                toolTitle.text = "Image Query"
                imageQueryTools = ImageQueryTools(queryViewModel, wasChecked, toolsContainer, this)
            }
            QueryTermType.AUDIO -> {
                toolTitle.text = "Audio Query"
                audioQueryTools = AudioQueryTools(queryViewModel, wasChecked, toolsContainer, this, {
                    getQueryContainerWithId(queryViewModel.currContainerID)?.performClick(QueryTermType.AUDIO)
                })
            }
            QueryTermType.MODEL3D -> {
                toolTitle.text = "3D Query"
                model3DQueryTools = Model3DQueryTools(queryViewModel, wasChecked, toolsContainer, this, {
                    getQueryContainerWithId(queryViewModel.currContainerID)?.performClick(QueryTermType.MODEL3D)
                })
            }
            QueryTermType.MOTION -> {
                toolTitle.text = "Motion Query"
                motionQueryTools = MotionQueryTools(queryViewModel, wasChecked, toolsContainer, this)
            }
            QueryTermType.TEXT -> {
                toolTitle.text = "Text Query"
                textQueryTools = TextQueryTools(queryViewModel, wasChecked, toolsContainer, this)
            }
            QueryTermType.LOCATION -> {
                toolTitle.text = "Location Query"
                locationQueryTools = LocationQueryTools(queryViewModel, wasChecked, toolsContainer, this, fusedLocationClient)
            }
        }
    }

    private fun setupTermSwitch(termType: QueryTermType) {
        enableTerm.setOnCheckedChangeListener(null)
        enableTerm.isChecked = true
        bottomSheetToggles.setChecked(termType, true)
        enableTerm.setOnCheckedChangeListener(enableTermListener)
    }

    private fun freeResources(type: QueryTermType, containerID: Long) {
        when (type) {
            QueryTermType.IMAGE -> {
                val preview = File(filesDir, "imageQuery_image_${containerID}_IMAGE.png")
                val orig = File(filesDir, "imageQuery_image_orig_${containerID}_IMAGE.png")
                if (preview.exists()) preview.delete()
                if (orig.exists()) orig.delete()
            }
            QueryTermType.AUDIO -> {
                val audioFile = File(filesDir, "audioQuery_recorded_audio_$containerID.wav")
                val loadedAudioFile = File(filesDir, "audioQuery_loaded_audio_$containerID.wav")
                if (audioFile.exists()) audioFile.delete()
                if (loadedAudioFile.exists()) loadedAudioFile.delete()
            }
            QueryTermType.MODEL3D -> {
                val preview = File(filesDir, "imageQuery_image_${containerID}_MODEL3D.png")
                val orig = File(filesDir, "imageQuery_image_orig_${containerID}_MODEL3D.png")
                if (preview.exists()) preview.delete()
                if (orig.exists()) orig.delete()
            }
            QueryTermType.MOTION -> {
                val preview = File(filesDir, "MOTION_QUERY_KEY_$containerID.png")
                if (preview.exists()) preview.delete()
                queryViewModel.removeMotionData(containerID)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            //freeing all resources
            for (container in queryViewModel.query.containers) {
                for (term in container.terms) {
                    freeResources(term.type, container.id)
                }
            }
        }

        // stop playing audio in case of query term AUDIO
        audioQueryTools?.stopPlayback()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {

            DRAWING_RESULT -> {
                if (resultCode == Activity.RESULT_OK)
                    imageQueryTools?.handleDrawingResult()
                getQueryContainerWithId(queryViewModel.currContainerID)?.performClick(QueryTermType.IMAGE)
            }

            RECORD_AUDIO_RESULT -> {
                if (resultCode == Activity.RESULT_OK) {
                    audioQueryTools?.handleRecordedAudioResult()
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    recordAudio.setColorFilter(ContextCompat.getColor(this@QueryActivity, R.color.tileIconSelected))
                    getQueryContainerWithId(queryViewModel.currContainerID)?.performClick(QueryTermType.AUDIO)
                }
            }

            LOAD_AUDIO_RESULT -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val filePathArray = ArrayList<String>(1)
                    filePathArray.addAll(data.getStringArrayListExtra(FilePickerConst.KEY_SELECTED_DOCS))
                    val filePath = filePathArray[0]
                    audioQueryTools?.handleLoadedAudioResult(filePath)
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    getQueryContainerWithId(queryViewModel.currContainerID)?.performClick(QueryTermType.AUDIO)
                }
            }

            MODEL_CHOOSER_RESULT -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val filePath = data.getStringExtra(com.nbsp.materialfilepicker.ui.FilePickerActivity.RESULT_FILE_PATH)
                    model3DQueryTools?.handleChosenModel(filePath)
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    getQueryContainerWithId(queryViewModel.currContainerID)?.performClick(QueryTermType.MODEL3D)
                }
            }

            MODEL_DRAWING_RESULT -> {
                if (resultCode == Activity.RESULT_OK)
                    model3DQueryTools?.handleDrawingResult()
                getQueryContainerWithId(queryViewModel.currContainerID)?.performClick(QueryTermType.MODEL3D)
            }

            MOTION_DRAW_RESULT -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val base64 = data.getStringExtra("base64")
                    motionQueryTools?.handleMotionDrawingResult(base64)
                }
                getQueryContainerWithId(queryViewModel.currContainerID)?.performClick(QueryTermType.MOTION)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            RECORD_REQUEST_CODE -> {
                if (permissions[0] == android.Manifest.permission.RECORD_AUDIO && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    audioQueryTools?.startRecordingAudio()
                }
            }
            LOAD_AUDIO_REQUEST_CODE -> {
                if (permissions[0] == android.Manifest.permission.WRITE_EXTERNAL_STORAGE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    audioQueryTools?.startPickingFile()
                }
            }
            MODEL_CHOOSER_REQUEST_CODE -> {
                if (permissions[0] == android.Manifest.permission.READ_EXTERNAL_STORAGE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    model3DQueryTools?.startModelChooserActivity()
                }
            }
            CURRENT_LOCATION_REQUEST_CODE -> {
                if (permissions[0] == android.Manifest.permission.ACCESS_FINE_LOCATION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationQueryTools?.setCurrentLocation()
                }
            }
        }
    }
}
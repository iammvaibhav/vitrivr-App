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
import android.view.WindowManager
import android.widget.CompoundButton
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import droidninja.filepicker.FilePickerConst
import kotlinx.android.synthetic.main.query_activity.*
import kotlinx.android.synthetic.main.query_detail_bottom_sheet.*
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.components.drawing.DrawingActivity
import org.vitrivr.vitrivrapp.components.drawing.MotionDrawingActivity
import org.vitrivr.vitrivrapp.data.model.enums.MessageType
import org.vitrivr.vitrivrapp.data.model.enums.QueryTermType
import org.vitrivr.vitrivrapp.data.model.query.QueryContainerModel
import org.vitrivr.vitrivrapp.features.addmedia.AddMediaActivity
import org.vitrivr.vitrivrapp.features.query.tools.*
import org.vitrivr.vitrivrapp.features.results.ResultsActivity
import org.vitrivr.vitrivrapp.features.settings.SettingsActivity
import org.vitrivr.vitrivrapp.utils.showToast
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

@Suppress("PrivatePropertyName")
class QueryActivity : AppCompatActivity() {

    private lateinit var queryViewModel: QueryViewModel
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    private lateinit var bottomSheetToggles: QueryToggles
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var imageQueryTools: ImageQueryTools? = null
    private var audioQueryTools: AudioQueryTools? = null
    private var model3DQueryTools: Model3DQueryTools? = null
    private var motionQueryTools: MotionQueryTools? = null
    private var textQueryTools: TextQueryTools? = null
    private var locationQueryTools: LocationQueryTools? = null

    /**
     * holds the current query container
     */
    private var currQueryContainer: QueryContainer? = null

    /**
     * This is a listener for switch in the bottom sheet.
     * When unchecked, it removes the tools for the current term type and frees the resources used by it
     * When checked, it prepares the bottom sheet for the current term type
     */
    private val enableTermListener = { _: CompoundButton, isChecked: Boolean ->
        if (!isChecked) {

            /**
             * visual changes
             */
            toolsContainer.removeAllViews()
            toolTitle.text = ""
            bottomSheetToggles.setChecked(queryViewModel.currTermType, false)
            currQueryContainer?.setChecked(queryViewModel.currTermType, false)

            /**
             * remove the data associated with this container's current query term from the QueryModel object
             */
            queryViewModel.removeQueryTermFromContainer(queryViewModel.currContainerID, queryViewModel.currTermType)

            /**
             * free the resources used by this container's current query term
             */
            freeResources(queryViewModel.currTermType, queryViewModel.currContainerID)

            /**
             * stop audio in case of term type AUDIO
             */
            if (audioQueryTools != null) {
                audioQueryTools?.stopPlayback()
            }

        } else {

            /**
             * visual changes
             */
            currQueryContainer?.setChecked(queryViewModel.currTermType, true)

            /**
             * prepare the bottom sheet
             */
            prepareBottomSheet(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.query_activity)

        /**
         * setting up the action bar
         */
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        /**
         * Don't show soft keyboard when the app starts (solves bug in certain Tablets. ex. Nexus C)
         */
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        /**
         * Initialize fusedLocationClient
         */
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        /**
         * obtain the QueryViewModel object for this activity from ViewModel Provider
         */
        queryViewModel = ViewModelProviders.of(this).get(QueryViewModel::class.java)

        /**
         * Checks if queryViewModel is newly constructed. If it is, un-mark it as new, and restore
         * the queryViewModel state from the savedInstanceState bundle
         *
         * This will help to use ViewModel between orientation changes and restore ViewModel in case
         * of activity kill in low memory situations
         */
        if (queryViewModel.isNewViewModel) {
            queryViewModel.isNewViewModel = false

            /**
             * restore queryViewModel state if exists.
             */
            savedInstanceState?.let {
                queryViewModel.restoreQueryViewModelState()
            }
        }

        /**
         * Setup bottom sheet
         */
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.skipCollapsed = true
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                /**
                 * If bottom sheet is collapsed, stop playing the music if it is currently playing
                 */
                if (newState == BottomSheetBehavior.STATE_HIDDEN)
                    audioQueryTools?.stopPlayback()
            }
        })

        /**
         * Setup bottom sheet toggles
         */
        bottomSheetToggles = bottomSheet.findViewById(R.id.queryToggles)
        bottomSheetToggles.addQueryTermClickListener { queryTerm, wasChecked ->
            /**
             * If a another query term is clicked in bottom sheet toggles,
             * stop playing the music if it is currently playing
             */
            audioQueryTools?.stopPlayback()

            queryViewModel.currTermType = queryTerm
            prepareBottomSheet(wasChecked)
            currQueryContainer?.setChecked(queryTerm, true)
        }

        if (queryViewModel.query.containers.isEmpty()) {
            /**
             * Fresh Query
             */
            addQueryContainer(null)
        } else {
            /**
             * restore UI state from queryViewModel. Add all the containers from query object
             */
            queryViewModel.query.containers.forEach {
                val newContainer = getNewQueryContainerWithId(it.id)
                restoreContainerUIState(newContainer, it)
                queryContainers.addView(newContainer)
                if (it.id == queryViewModel.currContainerID) {
                    currQueryContainer = newContainer
                }
            }

            /**
             * restore bottom sheet toggle state
             */
            queryViewModel.query.containers.find { it.id == queryViewModel.currContainerID }?.let {
                it.terms.forEach {
                    bottomSheetToggles.setChecked(it.type, true)
                }
            }
        }
    }

    /**
     * Invokes when user press add media button in toolbar
     * opens the add media activity
     */
    fun openAddMediaActivity(@Suppress("UNUSED_PARAMETER") view: View) {
        startActivity(Intent(this, AddMediaActivity::class.java))
    }

    /**
     * Invokes when user presses settings button in toolbar
     * opens the settings activity
     */
    fun openSettings(@Suppress("UNUSED_PARAMETER") view: View) {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    /**
     * Invokes when user presses the search button
     * Starts the results activity with Q_SIM query type
     * Also saves the query object so it can be used by results activity
     */
    fun search(@Suppress("UNUSED_PARAMETER") view: View) {
        val intent = Intent(this, ResultsActivity::class.java)
        intent.putExtra(ResultsActivity.QUERY_TYPE, MessageType.Q_SIM)
        queryViewModel.saveQueryViewModelState()
        startActivity(intent)
    }

    /**
     * Invoked when user pressed clear all button.
     * Clears all the containers.
     */
    fun clearAll(@Suppress("UNUSED_PARAMETER") view: View) {
        /**
         * frees the resources used by the containers
         */
        queryViewModel.query.containers.forEach {
            val containerId = it.id
            freeResources(QueryTermType.IMAGE, containerId)
            freeResources(QueryTermType.AUDIO, containerId)
            freeResources(QueryTermType.MODEL3D, containerId)
            freeResources(QueryTermType.MOTION, containerId)
        }

        /**
         * remove all containers, add a new container and reset bottom sheet toggles
         */
        queryContainers.removeAllViews()
        queryViewModel.query.containers.clear()
        addQueryContainer(null)
        for (term in QueryTermType.values())
            bottomSheetToggles.setChecked(term, false)
    }

    /**
     * adds a new QueryContainer to layout and model
     */
    fun addQueryContainer(@Suppress("UNUSED_PARAMETER") view: View?) {
        val containerId = queryViewModel.addContainer()
        queryContainers.addView(getNewQueryContainerWithId(containerId))
    }

    /**
     * Invokes when user presses the back button in bottom sheet
     * Collapses the bottom sheet
     */
    fun collapseBottomSheet(@Suppress("UNUSED_PARAMETER") view: View) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    /**
     * A QueryContainer is a view with query toggles and a description holder. This method constructs a
     * new container object with the provided containerID
     *
     * @param containerID ID to be set for this new container
     * @return QueryContainer with the given containerID
     */
    private fun getNewQueryContainerWithId(containerID: Long): QueryContainer {
        val newContainer = QueryContainer(this)

        /**
         * remove container from layout, model and then free the resources
         */
        newContainer.addDeleteQueryContainerListener {
            if (queryContainers.childCount > 1) {
                queryContainers.removeView(newContainer)
                queryViewModel.removeContainer(containerID)

                freeResources(QueryTermType.IMAGE, containerID)
                freeResources(QueryTermType.AUDIO, containerID)
                freeResources(QueryTermType.MODEL3D, containerID)
                freeResources(QueryTermType.MOTION, containerID)
            }
        }

        /**
         * set current state, prepare bottom sheet and expand bottom sheet
         */
        newContainer.addQueryTermClickListener { queryTerm, wasChecked ->
            queryViewModel.currContainerID = containerID
            queryViewModel.currTermType = queryTerm
            currQueryContainer = newContainer
            prepareBottomSheet(wasChecked)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        /**
         * save query description
         */
        newContainer.addQueryDescriptionChangeListener {
            queryViewModel.setQueryDescriptionOfContainer(containerID, it)
        }

        return newContainer
    }

    /**
     * Restores the UI state (description and toggles state) of a new QueryContainer
     * @param queryContainer newly Created QueryContainer of which UI state is to be restored
     * @param queryContainerModel container model to restore UI state from
     */
    private fun restoreContainerUIState(queryContainer: QueryContainer, queryContainerModel: QueryContainerModel) {
        queryContainer.setQueryDescription(queryContainerModel.description)
        for (term in queryContainerModel.terms) {
            queryContainer.setChecked(term.type, true)
        }
    }

    /**
     * prepares the bottom sheet for current container ID and current term type.
     *
     * @param wasChecked indicates if the current query term was already checked or user just clicked
     * a new query term
     */
    private fun prepareBottomSheet(wasChecked: Boolean) {
        val type = queryViewModel.currTermType
        /**
         * Check the toggleTerm Switch and the bottom sheet toggle
         */
        toggleTerm.setOnCheckedChangeListener(null)
        toggleTerm.isChecked = true

        QueryTermType.values().forEach { bottomSheetToggles.setChecked(it, false) }
        queryViewModel.query.containers.find { it.id == queryViewModel.currContainerID }?.let {
            it.terms.forEach {
                bottomSheetToggles.setChecked(it.type, true)
            }
        }
        bottomSheetToggles.setChecked(type, true)
        toggleTerm.setOnCheckedChangeListener(enableTermListener)

        toolsContainer.removeAllViews()

        /**
         * According to the query term, set the bottom sheet title and setup the query tools
         */
        when (type) {
            QueryTermType.IMAGE -> {
                toolTitle.text = getString(R.string.query_image)
                imageQueryTools = ImageQueryTools(queryViewModel, wasChecked, toolsContainer, this)
            }
            QueryTermType.AUDIO -> {
                toolTitle.text = getString(R.string.query_audio)
                audioQueryTools = AudioQueryTools(queryViewModel, wasChecked, toolsContainer, this)
            }
            QueryTermType.MODEL3D -> {
                toolTitle.text = getString(R.string.query_model3d)
                model3DQueryTools = Model3DQueryTools(queryViewModel, wasChecked, toolsContainer, this)
            }
            QueryTermType.MOTION -> {
                toolTitle.text = getString(R.string.query_motion)
                motionQueryTools = MotionQueryTools(queryViewModel, wasChecked, toolsContainer, this)
            }
            QueryTermType.TEXT -> {
                toolTitle.text = getString(R.string.query_text)
                textQueryTools = TextQueryTools(queryViewModel, wasChecked, toolsContainer, this)
            }
            QueryTermType.LOCATION -> {
                toolTitle.text = getString(R.string.query_location)
                locationQueryTools = LocationQueryTools(queryViewModel, wasChecked, toolsContainer, this, fusedLocationClient)
            }
        }
    }

    /**
     * Frees the resources used by 'type' QueryTermType of container with 'containerID'
     *
     * @param type QueryTermType of which resources are to be freed
     * @param containerID containerID of which resources are to be freed
     */
    private fun freeResources(type: QueryTermType, containerID: Long) {
        when (type) {
            QueryTermType.IMAGE -> {
                /**
                 * removes the preview and original query image if exist
                 */
                val preview = DrawingActivity.getResultantImageFile(this, containerID, type)
                val orig = DrawingActivity.getOriginalImageFile(this, containerID, type)
                if (preview.exists()) preview.delete()
                if (orig.exists()) orig.delete()
            }
            QueryTermType.AUDIO -> {
                /**
                 * removes the loaded or recorded audio files
                 */
                val audioFile = File(filesDir, "audioQuery_${containerID}_recorded_audio.wav")
                val loadedAudioFile = File(filesDir, "audioQuery_${containerID}_loaded_audio.wav")
                if (audioFile.exists()) audioFile.delete()
                if (loadedAudioFile.exists()) loadedAudioFile.delete()
            }
            QueryTermType.MODEL3D -> {
                /**
                 * removes the preview and original model query image if exist
                 */
                val preview = DrawingActivity.getResultantImageFile(this, containerID, type)
                val orig = DrawingActivity.getOriginalImageFile(this, containerID, type)
                if (preview.exists()) preview.delete()
                if (orig.exists()) orig.delete()
            }
            QueryTermType.MOTION -> {
                /**
                 * removes the preview motion image and motion data if exist
                 */
                val preview = MotionDrawingActivity.getResultantMotionImageFile(this, containerID)
                if (preview.exists()) preview.delete()
                queryViewModel.removeMotionData(containerID)
            }
            else -> {
                /** No resources to free */
            }
        }
    }

    override fun onStop() {
        super.onStop()

        /**
         * If activity is finishing
         */
        if (isFinishing) {
            /**
             * Free all the resources
             */
            for (container in queryViewModel.query.containers) {
                for (term in container.terms) {
                    freeResources(term.type, container.id)
                }
            }

            /**
             * remove queryViewModel state if exists
             */
            queryViewModel.removeQueryViewModelState()
        }

        /**
         * If activity is not changing configuration and is not finishing, save the query object
         */
        if (!isChangingConfigurations && !isFinishing) {
            queryViewModel.saveQueryViewModelState()
        }

        /**
         * stop playing audio in case the audio is playing
         */
        audioQueryTools?.stopPlayback()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
        /**
         * Result of image query sketch
         * Programmatically clicks IMAGE query term and if result is ok then delegates
         * handling result to image query tools
         */
            DRAWING_RESULT -> {
                currQueryContainer?.performClick(QueryTermType.IMAGE)

                if (resultCode == Activity.RESULT_OK)
                    imageQueryTools?.handleDrawingResult()
            }

        /**
         * Result of audio query recorded audio
         * Programmatically clicks AUDIO query term and if result is ok then delegates
         * handing result to audio query tools
         */
            RECORD_AUDIO_RESULT -> {
                currQueryContainer?.performClick(QueryTermType.AUDIO)

                if (resultCode == Activity.RESULT_OK) {
                    audioQueryTools?.handleRecordedAudioResult()
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    audioQueryTools?.recordAudio?.setColorFilter(ContextCompat.getColor(this@QueryActivity, R.color.tileIconSelected))
                }
            }

        /**
         * Result of audio query loaded audio
         * Programmatically clicks AUDIO query term and if result is ok then delegates
         * handing result to audio query tools
         */
            LOAD_AUDIO_RESULT -> {
                currQueryContainer?.performClick(QueryTermType.AUDIO)

                if (resultCode == Activity.RESULT_OK && data != null) {
                    val filePath = data.getStringArrayListExtra(FilePickerConst.KEY_SELECTED_DOCS)[0]
                    audioQueryTools?.handleLoadedAudioResult(filePath)
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    audioQueryTools?.loadAudio?.setColorFilter(ContextCompat.getColor(this@QueryActivity, R.color.tileIconSelected))
                }
            }

        /**
         * Result of model3d query loaded model
         * Programmatically clicks MODEL3D query term and if result is ok then delegates
         * handing result to model3d query tools
         */
            MODEL_CHOOSER_RESULT -> {
                currQueryContainer?.performClick(QueryTermType.MODEL3D)

                if (resultCode == Activity.RESULT_OK && data != null) {
                    val filePath = data.getStringExtra(com.nbsp.materialfilepicker.ui.FilePickerActivity.RESULT_FILE_PATH)
                    model3DQueryTools?.handleChosenModel(filePath)
                }
            }

        /**
         * Result of model3d query model drawing
         * Programmatically clicks MODEL3D query term and if result is ok then delegates
         * handing result to model3d query tools
         */
            MODEL_DRAWING_RESULT -> {
                currQueryContainer?.performClick(QueryTermType.MODEL3D)

                if (resultCode == Activity.RESULT_OK)
                    model3DQueryTools?.handleDrawingResult()
            }

        /**
         * Result of motion query drawing
         * Programmatically clicks MOTION query term and if result is ok then delegates
         * handing result to motion query tools
         */
            MOTION_DRAW_RESULT -> {
                currQueryContainer?.performClick(QueryTermType.MOTION)

                if (resultCode == Activity.RESULT_OK && data != null) {
                    val base64 = data.getStringExtra("base64")
                    motionQueryTools?.handleMotionDrawingResult(base64)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
        /**
         * start recording audio if permissions are granted
         */
            RECORD_REQUEST_CODE -> {
                if (permissions.isNotEmpty() && permissions[0] == android.Manifest.permission.RECORD_AUDIO
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    audioQueryTools?.startRecordingAudio()
                } else {
                    "Recording Permission is required for recording audio".showToast(this)
                }
            }

        /**
         * start picking audio file if permission is granted
         */
            LOAD_AUDIO_REQUEST_CODE -> {
                if (permissions.isNotEmpty() && permissions[0] == android.Manifest.permission.READ_EXTERNAL_STORAGE
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    audioQueryTools?.startPickingFile()
                } else {
                    "External storage Read Permission is required for selecting audio".showToast(this)
                }
            }

        /**
         * start choosing model if permission is granted
         */
            MODEL_CHOOSER_REQUEST_CODE -> {
                if (permissions.isNotEmpty() && permissions[0] == android.Manifest.permission.READ_EXTERNAL_STORAGE
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    model3DQueryTools?.startModelChooserActivity()
                } else {
                    "External storage Read Permission is required for selecting model".showToast(this)
                }
            }

        /**
         * set current location in location query tools if permission is granted
         */
            CURRENT_LOCATION_REQUEST_CODE -> {
                if (permissions.isNotEmpty() && permissions[0] == android.Manifest.permission.ACCESS_FINE_LOCATION
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationQueryTools?.setCurrentLocation()
                } else {
                    "Location permission is required for retrieving current location".showToast(this)
                }
            }
        }
    }
}
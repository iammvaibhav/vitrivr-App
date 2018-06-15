package org.vitrivr.vitrivrapp.features.query

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.widget.NestedScrollView
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import cafe.adriel.androidaudiorecorder.AndroidAudioRecorder
import cafe.adriel.androidaudiorecorder.model.AudioChannel
import cafe.adriel.androidaudiorecorder.model.AudioSampleRate
import cafe.adriel.androidaudiorecorder.model.AudioSource
import droidninja.filepicker.FilePickerBuilder
import droidninja.filepicker.FilePickerConst
import kotlinx.android.synthetic.main.audio_query_tools.*
import kotlinx.android.synthetic.main.query_activity.*
import kotlinx.android.synthetic.main.query_detail_bottom_sheet.*
import nl.bravobit.ffmpeg.FFcommandExecuteResponseHandler
import nl.bravobit.ffmpeg.FFmpeg
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.components.drawing.DrawingActivity
import org.vitrivr.vitrivrapp.components.drawing.DrawingActivity.Companion.IMAGE_PATH
import org.vitrivr.vitrivrapp.data.model.enums.QueryTermType
import org.vitrivr.vitrivrapp.data.model.query.QueryTermModel
import org.vitrivr.vitrivrapp.features.results.ResultsActivity
import org.vitrivr.vitrivrapp.features.settings.SettingsActivity
import org.vitrivr.vitrivrapp.utils.px
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream


class QueryActivity : AppCompatActivity() {

    private val DRAWING_RESULT = 1
    private val RECORD_AUDIO_RESULT = 2
    private val LOAD_AUDIO_RESULT = 3

    private val RECORD_REQUEST_CODE = 1
    private val LOAD_AUDIO_REQUEST_CODE = 2

    private val CURR_CONTAINER_ID = "CURR_CONTAINER_ID"
    private val CURR_TERM_TYPE = "CURR_TERM_TYPE"

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    private lateinit var queryViewModel: QueryViewModel
    private lateinit var bottomSheetToggles: QueryToggles

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
            queryViewModel.currTermType = queryTerm
            prepareBottomSheet(queryTerm, wasChecked, queryViewModel.currContainerID)
            getQueryContainerWithId(queryViewModel.currContainerID)?.setChecked(queryTerm, true)
            //bottomSheetToggles.setChecked(queryTerm, false)
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

    fun collapseBottomSheet(view: View) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    fun search(view: View) {
        val intent = Intent(this, ResultsActivity::class.java)
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
                LayoutInflater.from(this).inflate(R.layout.image_query_tools, toolsContainer, true)
                setupImageTools(wasChecked, containerId)

            }
            QueryTermType.AUDIO -> {
                toolTitle.text = "Audio Query"
                LayoutInflater.from(this).inflate(R.layout.audio_query_tools, toolsContainer, true)
                setupAudioTools(wasChecked, containerId)
            }
        //TODO(Others)
        }
    }

    private fun setupTermSwitch(termType: QueryTermType) {
        enableTerm.setOnCheckedChangeListener(null)
        enableTerm.isChecked = true
        bottomSheetToggles.setChecked(termType, true)
        enableTerm.setOnCheckedChangeListener(enableTermListener)
    }

    private fun setupImageTools(wasChecked: Boolean, containerId: Long) {

        val drawImageBalance = toolsContainer.findViewById<SeekBar>(R.id.drawImageBalance)
        val previewImage = toolsContainer.findViewById<ImageView>(R.id.imagePreview)

        previewImage.setOnClickListener {
            val intent = Intent(this, DrawingActivity::class.java)
            intent.putExtra("containerID", queryViewModel.currContainerID)
            startActivityForResult(intent, DRAWING_RESULT)
        }

        drawImageBalance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                queryViewModel.setBalance(containerId, QueryTermType.IMAGE, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


        if (wasChecked) {
            // restore state
            drawImageBalance.progress = queryViewModel.getBalance(containerId, QueryTermType.IMAGE)
            val image = BitmapFactory.decodeFile(File(filesDir, "imageQuery_image_${queryViewModel.currContainerID}.png").absolutePath)
            previewImage.setImageBitmap(image)
        } else {
            queryViewModel.addQueryTermToContainer(containerId, QueryTermType.IMAGE)
        }
    }

    private fun setupAudioTools(wasChecked: Boolean, containerId: Long) {
        val recordAudio = toolsContainer.findViewById<ImageView>(R.id.recordAudio)
        val removeAudio = toolsContainer.findViewById<ImageView>(R.id.removeAudio)
        val audioName = toolsContainer.findViewById<TextView>(R.id.audioName)
        val loadAudio = toolsContainer.findViewById<ImageView>(R.id.loadAudio)
        val playAudio = toolsContainer.findViewById<ImageView>(R.id.playAudio)
        val audioDuration = toolsContainer.findViewById<TextView>(R.id.audioDuration)
        val fingerprintHummingBalance = toolsContainer.findViewById<SeekBar>(R.id.fingerprintHummingBalance)

        recordAudio.setOnClickListener {
            recordAudio.setColorFilter(Color.RED)
            val filePath = File(filesDir, "audioQuery_recorded_audio_${containerId}_unformatted.wav").absolutePath
            val color = ContextCompat.getColor(this, R.color.colorPrimaryDark)

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), RECORD_REQUEST_CODE)
            } else {
                AndroidAudioRecorder.with(this)
                        .setFilePath(filePath)
                        .setColor(color)
                        .setRequestCode(RECORD_AUDIO_RESULT)
                        .setSource(AudioSource.MIC)
                        .setChannel(AudioChannel.STEREO)
                        .setSampleRate(AudioSampleRate.HZ_48000)
                        .setAutoStart(true)
                        .setKeepDisplayOn(true)
                        .record()
            }
        }

        removeAudio.setOnClickListener {
            val recordedAudioFile = File(filesDir, "audioQuery_recorded_audio_$containerId.wav")
            val loadedAudioFile = File(filesDir, "audioQuery_loaded_audio_$containerId.wav")

            if (recordedAudioFile.exists())
                recordedAudioFile.delete()

            if (loadedAudioFile.exists())
                loadedAudioFile.delete()

            recordAudio.setColorFilter(ContextCompat.getColor(this, R.color.tileIconSelected))
            loadAudio.setColorFilter(ContextCompat.getColor(this, R.color.tileIconSelected))
            audioDuration.text = "0:00"
            audioName.text = "No Audio Available"
        }

        loadAudio.setOnClickListener {
            loadAudio.setColorFilter(Color.RED)

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), LOAD_AUDIO_REQUEST_CODE)
            } else {
                val filePath = ArrayList<String>(1)
                FilePickerBuilder.getInstance()
                        .setMaxCount(1)
                        .setSelectedFiles(filePath)
                        .setActivityTheme(R.style.LibAppTheme)
                        .addFileSupport("Audio", arrayOf("aac", "mp3", "m4a", "wma", "wav", "flac"))
                        .pickFile(this, LOAD_AUDIO_RESULT)
            }
        }

        playAudio.setOnClickListener {

        }

        fingerprintHummingBalance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                queryViewModel.setBalance(containerId, QueryTermType.AUDIO, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        if (wasChecked) {
            // restore state
            fingerprintHummingBalance.progress = queryViewModel.getBalance(containerId, QueryTermType.AUDIO)

            val recordedAudioFile = File(filesDir, "audioQuery_recorded_audio_$containerId.wav")
            if (recordedAudioFile.exists()) {
                recordAudio.setColorFilter(Color.RED)
                audioName.text = "Audio Recorded"

                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(recordedAudioFile.absolutePath)
                val durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

                val minutes = (durationStr.toLong() / 1000) / 60
                val seconds = (durationStr.toLong() / 1000) % 60
                val duration = "$minutes:${String.format("%02d", seconds)}"
                audioDuration.text = duration

            } else {
                recordAudio.setColorFilter(ContextCompat.getColor(this, R.color.tileIconSelected))
            }

            val loadedAudioFile = File(filesDir, "audioQuery_loaded_audio_$containerId.wav")

            if (loadedAudioFile.exists()) {
                loadAudio.setColorFilter(Color.RED)
                audioName.text = "Audio Loaded"

                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(loadedAudioFile.absolutePath)
                val durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

                val minutes = (durationStr.toLong() / 1000) / 60
                val seconds = (durationStr.toLong() / 1000) % 60
                val duration = "$minutes:${String.format("%02d", seconds)}"
                audioDuration.text = duration

            } else {
                loadAudio.setColorFilter(ContextCompat.getColor(this, R.color.tileIconSelected))
            }

        } else {
            queryViewModel.addQueryTermToContainer(containerId, QueryTermType.AUDIO)
        }


    }

    private fun freeResources(type: QueryTermType, containerID: Long) {
        when (type) {
            QueryTermType.IMAGE -> {
                val preview = File(filesDir, "imageQuery_image_$containerID.png")
                val orig = File(filesDir, "imageQuery_image_orig_$containerID.png")
                if (preview.exists()) preview.delete()
                if (orig.exists()) orig.delete()
            }
            QueryTermType.AUDIO -> {
                val audioFile = File(filesDir, "audioQuery_recorded_audio_$containerID.wav")
                val loadedAudioFile = File(filesDir, "audioQuery_loaded_audio_$containerID.wav")
                if (audioFile.exists()) audioFile.delete()
                if (loadedAudioFile.exists()) loadedAudioFile.delete()
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
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            DRAWING_RESULT -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val image = BitmapFactory.decodeFile(data.getStringExtra(IMAGE_PATH))
                    val outputStream = ByteArrayOutputStream()
                    image.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                    queryViewModel.setDataOfQueryTerm(queryViewModel.currContainerID, QueryTermType.IMAGE, base64String)
                    getQueryContainerWithId(queryViewModel.currContainerID)?.performClick(QueryTermType.IMAGE)
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    getQueryContainerWithId(queryViewModel.currContainerID)?.performClick(QueryTermType.IMAGE)
                }
            }
            RECORD_AUDIO_RESULT -> {
                if (resultCode == Activity.RESULT_OK) {
                    loadAudio.setColorFilter(ContextCompat.getColor(this@QueryActivity, R.color.tileIconSelected))
                    val loadedAudio = File(filesDir, "audioQuery_loaded_audio_${queryViewModel.currContainerID}.wav")
                    if (loadedAudio.exists()) loadedAudio.delete()

                    val unformattedAudioFile = File(filesDir, "audioQuery_recorded_audio_${queryViewModel.currContainerID}_unformatted.wav")
                    val audioFile = File(filesDir, "audioQuery_recorded_audio_${queryViewModel.currContainerID}.wav")

                    if (FFmpeg.getInstance(this).isSupported) {
                        FFmpeg.getInstance(this).execute(arrayOf("-i", unformattedAudioFile.absolutePath, "-ar", "22050", "-map_channel", "0.0.0", audioFile.absolutePath), object : FFcommandExecuteResponseHandler {
                            override fun onFinish() {

                            }

                            override fun onSuccess(message: String?) {
                                recordAudio.setColorFilter(ContextCompat.getColor(this@QueryActivity, R.color.tileIconSelected))
                                val recordedAudio = File(filesDir, "audioQuery_recorded_audio_${queryViewModel.currContainerID}.wav")
                                if (recordedAudio.exists()) recordedAudio.delete()
                                unformattedAudioFile.delete()

                                val fileStream = FileInputStream(audioFile)
                                val byteArrayOutputStream = ByteArrayOutputStream()
                                val byteArray = ByteArray(1024)
                                var x = fileStream.read(byteArray)
                                while (x != -1) {
                                    byteArrayOutputStream.write(byteArray, 0, x)
                                    x = fileStream.read(byteArray)
                                }
                                val base64String = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)
                                queryViewModel.setDataOfQueryTerm(queryViewModel.currContainerID, QueryTermType.AUDIO, base64String)
                                getQueryContainerWithId(queryViewModel.currContainerID)?.performClick(QueryTermType.AUDIO)
                            }

                            override fun onFailure(message: String?) {

                            }

                            override fun onProgress(message: String?) {

                            }

                            override fun onStart() {

                            }
                        })
                    } else {
                        Toast.makeText(this, "FFmpeg not supported", Toast.LENGTH_SHORT).show()
                    }

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
                    val audioFile = File(filePath)
                    val loadedAudioFile = File(filesDir, "audioQuery_loaded_audio_${queryViewModel.currContainerID}.wav")

                    if (FFmpeg.getInstance(this).isSupported) {
                        FFmpeg.getInstance(this).execute(arrayOf("-i", filePath, "-ar", "22050", "-map_channel", "0.0.0", loadedAudioFile.absolutePath), object : FFcommandExecuteResponseHandler {
                            override fun onFinish() {

                            }

                            override fun onSuccess(message: String?) {
                                recordAudio.setColorFilter(ContextCompat.getColor(this@QueryActivity, R.color.tileIconSelected))
                                val recordedAudio = File(filesDir, "audioQuery_recorded_audio_${queryViewModel.currContainerID}.wav")
                                if (recordedAudio.exists()) recordedAudio.delete()

                                val fileStream = FileInputStream(loadedAudioFile)
                                val byteArrayOutputStream = ByteArrayOutputStream()
                                val byteArray = ByteArray(1024)
                                var x = fileStream.read(byteArray)
                                while (x != -1) {
                                    byteArrayOutputStream.write(byteArray, 0, x)
                                    x = fileStream.read(byteArray)
                                }
                                val base64String = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)

                                queryViewModel.setDataOfQueryTerm(queryViewModel.currContainerID, QueryTermType.AUDIO, base64String)
                                getQueryContainerWithId(queryViewModel.currContainerID)?.performClick(QueryTermType.AUDIO)
                            }

                            override fun onFailure(message: String?) {

                            }

                            override fun onProgress(message: String?) {

                            }

                            override fun onStart() {

                            }
                        })
                    } else {
                        Toast.makeText(this, "FFmpeg not supported", Toast.LENGTH_SHORT).show()
                    }

                } else if (resultCode == Activity.RESULT_CANCELED) {
                    getQueryContainerWithId(queryViewModel.currContainerID)?.performClick(QueryTermType.AUDIO)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            RECORD_REQUEST_CODE -> {
                if (permissions[0] == android.Manifest.permission.RECORD_AUDIO && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    val filePath = File(filesDir, "audioQuery_recorded_audio_${queryViewModel.currContainerID}_unformatted.wav").absolutePath
                    val color = ContextCompat.getColor(this, R.color.colorPrimaryDark)

                    AndroidAudioRecorder.with(this)
                            .setFilePath(filePath)
                            .setColor(color)
                            .setRequestCode(RECORD_AUDIO_RESULT)
                            .setSource(AudioSource.MIC)
                            .setChannel(AudioChannel.STEREO)
                            .setSampleRate(AudioSampleRate.HZ_48000)
                            .setAutoStart(true)
                            .setKeepDisplayOn(true)
                            .record()
                }
            }
            LOAD_AUDIO_REQUEST_CODE -> {
                if (permissions[0] == android.Manifest.permission.WRITE_EXTERNAL_STORAGE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    val filePath = ArrayList<String>(1)
                    FilePickerBuilder.getInstance()
                            .setMaxCount(1)
                            .setSelectedFiles(filePath)
                            .setActivityTheme(R.style.LibAppTheme)
                            .addFileSupport("Audio", arrayOf("aac", "mp3", "m4a", "wma", "wav", "flac"))
                            .pickFile(this, LOAD_AUDIO_RESULT)

                }
            }
        }
    }
}
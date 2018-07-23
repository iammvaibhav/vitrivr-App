package org.vitrivr.vitrivrapp.features.addmedia

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import droidninja.filepicker.FilePickerBuilder
import droidninja.filepicker.FilePickerConst
import kotlinx.android.synthetic.main.add_media_activity.*
import net.gotev.uploadservice.MultipartUploadRequest
import net.gotev.uploadservice.UploadNotificationConfig
import org.json.JSONObject
import org.vitrivr.vitrivrapp.App
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.components.results.EqualSpacingItemDecoration
import org.vitrivr.vitrivrapp.data.model.addmedia.ExtractionConfig
import org.vitrivr.vitrivrapp.data.model.addmedia.ExtractionItem
import org.vitrivr.vitrivrapp.data.model.addmedia.ExtractionObject
import org.vitrivr.vitrivrapp.data.model.enums.MediaType
import org.vitrivr.vitrivrapp.data.services.SettingsService
import org.vitrivr.vitrivrapp.utils.px
import java.io.File
import java.io.FileWriter
import javax.inject.Inject

class AddMediaActivity : AppCompatActivity() {

    val IMAGE_REQUEST_CODE = 1
    val VIDEO_REQUEST_CODE = 2
    val AUDIO_REQUEST_CODE = 3
    val MODEL3D_REQUEST_CODE = 4

    val READ_EXTERNAL_REQUEST_CODE = 100

    val EXTRACTION_CONFIG_KEY = "EXTRACTION_CONFIG_KEY"
    val MEDIA_TYPE_KEY = "MEDIA_TYPE_KEY"
    val PATHS_KEY = "PATHS_KEY"

    var extractionConfig = ExtractionConfig()
    var mediaType: MediaType? = null
    var paths = ArrayList<String>()

    @Inject
    lateinit var settingsService: SettingsService

    init {
        App.daggerAppComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_media_activity)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)

        if (savedInstanceState != null) {
            extractionConfig = savedInstanceState.getParcelable(EXTRACTION_CONFIG_KEY)
            mediaType = savedInstanceState.getSerializable(MEDIA_TYPE_KEY) as MediaType
            paths = savedInstanceState.getStringArrayList(PATHS_KEY)
        }

        itemsRecyclerView.layoutManager = LinearLayoutManager(this)
        itemsRecyclerView.adapter = SelectedItemsAdapter(extractionConfig)
        itemsRecyclerView.addItemDecoration(EqualSpacingItemDecoration(6.px))

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                extractionConfig.items.removeAt(pos)
                paths.removeAt(pos)
                (itemsRecyclerView.adapter as SelectedItemsAdapter).update()
            }
        }

        // attaching the touch helper to recycler view
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(itemsRecyclerView)

        if (mediaType != null) {
            when (mediaType) {
                MediaType.IMAGE -> type_image.isChecked = true
                MediaType.VIDEO -> type_video.isChecked = true
                MediaType.AUDIO -> type_audio.isChecked = true
                MediaType.MODEL3D -> type_3d.isChecked = true
            }
        }

        fileType.setOnCheckedChangeListener { _, _ ->
            extractionConfig.items.clear()
            paths.clear()
            mediaType = null
            (itemsRecyclerView.adapter as SelectedItemsAdapter).update()
        }

        selectFiles.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), READ_EXTERNAL_REQUEST_CODE)
            } else {
                startSelectingFiles()
            }
        }

        uploadAndExtract.setOnClickListener {
            if (paths.isEmpty()) {
                Toast.makeText(this, "Please select at least one file.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val serverSettings = settingsService.getCineastAPISettings()
            if (serverSettings != null) {
                val uploadAddress = "http://${serverSettings.address}:${serverSettings.port}/api/v1/extractFiles"
                Log.e("config", JSONObject(Gson().toJson(extractionConfig)).toString(4))
                Log.e("paths", paths.toString())

                val configFile = File(filesDir, "extract_config.json")
                val output = FileWriter(configFile)
                output.write(Gson().toJson(extractionConfig))
                output.flush()
                output.close()

                try {
                    var multipartUploadRequest = MultipartUploadRequest(this, uploadAddress)
                    multipartUploadRequest.addFileToUpload(configFile.absolutePath, "extract_config")
                    paths.forEach {
                        val filename = File(it).name
                        multipartUploadRequest = multipartUploadRequest.addFileToUpload(it, filename, filename)
                    }

                    multipartUploadRequest.addHeader("media_type", mediaType?.name)
                            .setNotificationConfig(UploadNotificationConfig())
                            .setMaxRetries(2)
                            .startUpload()

                    Toast.makeText(this, "Upload started. Check notification for upload status", Toast.LENGTH_SHORT).show()
                    finish()

                } catch (e: Exception) {
                    Log.e("AndroidUploadService", e.message, e)
                }
            } else {
                Toast.makeText(this, "Server Settings not found. Please configure it first.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
            val key = when (requestCode) {
                IMAGE_REQUEST_CODE -> FilePickerConst.KEY_SELECTED_MEDIA
                VIDEO_REQUEST_CODE -> FilePickerConst.KEY_SELECTED_MEDIA
                MODEL3D_REQUEST_CODE -> FilePickerConst.KEY_SELECTED_DOCS
                AUDIO_REQUEST_CODE -> FilePickerConst.KEY_SELECTED_DOCS
                else -> ""
            }
            paths.clear()
            paths.addAll(data.getStringArrayListExtra(key))
            constructExtractionConfig()
            (itemsRecyclerView.adapter as SelectedItemsAdapter).update()
        } else {
            Toast.makeText(this, "Selection Cancelled", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            READ_EXTERNAL_REQUEST_CODE -> {
                if (permissions[0] == android.Manifest.permission.READ_EXTERNAL_STORAGE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startSelectingFiles()
                } else {
                    Toast.makeText(this, "Read permissions is required", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.let {
            it.putParcelable(EXTRACTION_CONFIG_KEY, extractionConfig)
            it.putSerializable(MEDIA_TYPE_KEY, mediaType)
            it.putStringArrayList(PATHS_KEY, paths)
        }
    }

    private fun constructExtractionConfig() {
        extractionConfig.items.clear()
        extractionConfig.items.addAll(paths.map {
            val file = File(it)
            return@map ExtractionItem(ExtractionObject(file.name, file.name, mediaType!!), ArrayList())
        })
    }

    private fun startSelectingFiles() {
        when (fileType.checkedRadioButtonId) {
            R.id.type_image -> {
                FilePickerBuilder.getInstance()
                        .setActivityTheme(R.style.LibAppTheme)
                        .setActivityTitle("Select Images")
                        .setSelectedFiles(if (mediaType != MediaType.IMAGE) ArrayList() else paths)
                        .enableSelectAll(true)
                        .enableVideoPicker(false)
                        .enableImagePicker(true)
                        .pickPhoto(this, IMAGE_REQUEST_CODE)
                mediaType = MediaType.IMAGE
            }

            R.id.type_video -> {
                FilePickerBuilder.getInstance()
                        .setActivityTheme(R.style.LibAppTheme)
                        .setActivityTitle("Select Videos")
                        .setSelectedFiles(if (mediaType != MediaType.VIDEO) ArrayList() else paths)
                        .enableSelectAll(true)
                        .enableImagePicker(false)
                        .enableVideoPicker(true)
                        .pickPhoto(this, VIDEO_REQUEST_CODE)
                mediaType = MediaType.VIDEO
            }

            R.id.type_audio -> {
                FilePickerBuilder.getInstance()
                        .setActivityTheme(R.style.LibAppTheme)
                        .setActivityTitle("Select Audio")
                        .setSelectedFiles(if (mediaType != MediaType.AUDIO) ArrayList() else paths)
                        .enableSelectAll(true)
                        .enableDocSupport(false)
                        .addFileSupport("Audio", arrayOf("aac", "mp3", "m4a", "wma", "wav", "flac"))
                        .pickFile(this, AUDIO_REQUEST_CODE)
                mediaType = MediaType.AUDIO
            }

            R.id.type_3d -> {
                FilePickerBuilder.getInstance()
                        .setActivityTheme(R.style.LibAppTheme)
                        .setActivityTitle("Select 3D Models")
                        .setSelectedFiles(if (mediaType != MediaType.MODEL3D) ArrayList() else paths)
                        .enableSelectAll(true)
                        .addFileSupport("3D Models", arrayOf(".stl", ".obj"))
                        .enableDocSupport(false)
                        .pickFile(this, MODEL3D_REQUEST_CODE)
                mediaType = MediaType.MODEL3D
            }

            -1 -> Snackbar.make(rootView, "Select File Type before selecting files", Snackbar.LENGTH_SHORT).show()
        }
    }
}
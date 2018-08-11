package org.vitrivr.vitrivrapp.features.resultdetails

import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.indicator.progresspie.ProgressPieIndicator
import com.github.piasy.biv.loader.glide.GlideImageLoader
import com.github.piasy.biv.view.ImageSaveCallback
import kotlinx.android.synthetic.main.image_result_detail_activity.*
import org.vitrivr.vitrivrapp.App
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.components.results.EqualSpacingItemDecoration
import org.vitrivr.vitrivrapp.data.model.enums.MediaType
import org.vitrivr.vitrivrapp.data.model.results.QueryResultPresenterModel
import org.vitrivr.vitrivrapp.features.results.ResultsActivity.Companion.CATEGORY_INFO
import org.vitrivr.vitrivrapp.features.results.ResultsActivity.Companion.PRESENTER_OBJECT
import org.vitrivr.vitrivrapp.utils.PathUtils
import org.vitrivr.vitrivrapp.utils.px
import javax.inject.Inject


class ImageResultDetailActivity : AppCompatActivity() {

    @Inject
    lateinit var pathUtils: PathUtils
    val WRITE_REQUEST_CODE = 1

    lateinit var presenterObject: QueryResultPresenterModel
    lateinit var categoryInfo: HashMap<MediaType, HashSet<String>>
    lateinit var featureInfoDialog: AlertDialog
    lateinit var infoFileName: TextView
    lateinit var infoFilePath: TextView
    lateinit var infoObjectId: TextView
    lateinit var infoMediaType: TextView
    lateinit var allSegmentsRV: RecyclerView

    init {
        App.daggerAppComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BigImageViewer.initialize(GlideImageLoader.with(applicationContext))

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.image_result_detail_activity)

        if (savedInstanceState == null) {
            presenterObject = intent.getParcelableExtra(PRESENTER_OBJECT)
            categoryInfo = intent.getSerializableExtra(CATEGORY_INFO) as HashMap<MediaType, HashSet<String>>
        } else {
            presenterObject = savedInstanceState.getParcelable(PRESENTER_OBJECT)
            categoryInfo = savedInstanceState.getSerializable(CATEGORY_INFO) as HashMap<MediaType, HashSet<String>>
        }

        val uri = pathUtils.getObjectURI(presenterObject)

        val view = LayoutInflater.from(this).inflate(R.layout.object_info, null)

        featureInfoDialog = AlertDialog.Builder(this)
                .setTitle("Info")
                .setView(view)
                .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
                .create()

        infoFileName = view.findViewById(R.id.fileName)
        infoFilePath = view.findViewById(R.id.path)
        infoObjectId = view.findViewById(R.id.id)
        infoMediaType = view.findViewById(R.id.mediaType)
        allSegmentsRV = view.findViewById(R.id.allSegmentsRV)

        infoFileName.text = presenterObject.fileName
        infoFilePath.text = presenterObject.filePath
        infoObjectId.text = presenterObject.objectId
        infoMediaType.text = presenterObject.mediaType.name

        allSegmentsRV.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        allSegmentsRV.addItemDecoration(EqualSpacingItemDecoration(4.px, EqualSpacingItemDecoration.HORIZONTAL))
        allSegmentsRV.adapter = AllSegmentsAdapter(presenterObject.allSegments,
                presenterObject.objectId,
                presenterObject.mediaType,
                categoryInfo)

        bigImage.setProgressIndicator(ProgressPieIndicator())
        bigImage.showImage(uri)
        bigImage.setImageSaveCallback(object : ImageSaveCallback {
            override fun onSuccess(uri: String?) {
                Snackbar.make(root, "Image saved to Gallery", Snackbar.LENGTH_SHORT).show()
                Log.e("URI", uri.toString())
            }

            override fun onFail(t: Throwable?) {
                Snackbar.make(root, "Image saved to Gallery failed", Snackbar.LENGTH_SHORT).show()
                t?.printStackTrace()
            }
        })

    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState?.let {
            outState.putParcelable(PRESENTER_OBJECT, presenterObject)
            outState.putSerializable(CATEGORY_INFO, categoryInfo)
        }
    }

    fun info(view: View) {
        featureInfoDialog.show()
    }

    fun save(view: View) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_REQUEST_CODE)
        } else {
            bigImage.saveImageIntoGallery()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (permissions.isNotEmpty() && requestCode == WRITE_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        }
    }
}
package org.vitrivr.vitrivrapp.features.resultdetails

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.ContentResolverCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.*
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.dmitrybrant.modelviewer.Model
import com.dmitrybrant.modelviewer.ModelSurfaceView
import com.dmitrybrant.modelviewer.ModelViewerApplication
import com.dmitrybrant.modelviewer.gvr.ModelGvrActivity
import com.dmitrybrant.modelviewer.obj.ObjModel
import com.dmitrybrant.modelviewer.ply.PlyModel
import com.dmitrybrant.modelviewer.stl.StlModel
import com.dmitrybrant.modelviewer.util.Util
import okhttp3.OkHttpClient
import okhttp3.Request
import org.vitrivr.vitrivrapp.App
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.components.results.EqualSpacingItemDecoration
import org.vitrivr.vitrivrapp.data.model.enums.MediaType
import org.vitrivr.vitrivrapp.data.model.results.QueryResultPresenterModel
import org.vitrivr.vitrivrapp.features.results.ResultsActivity.Companion.CATEGORY_INFO
import org.vitrivr.vitrivrapp.features.results.ResultsActivity.Companion.PRESENTER_OBJECT
import org.vitrivr.vitrivrapp.utils.PathUtils
import org.vitrivr.vitrivrapp.utils.px
import java.io.ByteArrayInputStream
import java.io.InputStream
import javax.inject.Inject

class Model3DResultDetailActivity : AppCompatActivity() {

    @Inject
    lateinit var pathUtils: PathUtils

    private lateinit var app: ModelViewerApplication
    private lateinit var containerView: ViewGroup
    private lateinit var progressBar: ProgressBar
    private lateinit var infoSaveContainer: LinearLayout
    private var modelView: ModelSurfaceView? = null

    lateinit var presenterObject: QueryResultPresenterModel
    lateinit var categoryInfo: HashMap<MediaType, HashSet<String>>
    lateinit var featureInfoDialog: AlertDialog
    lateinit var infoFileName: TextView
    lateinit var infoFilePath: TextView
    lateinit var infoObjectId: TextView
    lateinit var infoMediaType: TextView
    lateinit var allSegmentsRV: RecyclerView

    private var uri: Uri? = null

    init {
        App.daggerAppComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.model_3d_result_detail_activity)

        if (savedInstanceState == null) {
            presenterObject = intent.getParcelableExtra(PRESENTER_OBJECT)
            categoryInfo = intent.getSerializableExtra(CATEGORY_INFO) as HashMap<MediaType, HashSet<String>>
        } else {
            presenterObject = savedInstanceState.getParcelable(PRESENTER_OBJECT)
            categoryInfo = savedInstanceState.getSerializable(CATEGORY_INFO) as HashMap<MediaType, HashSet<String>>
        }

        app = ModelViewerApplication.getInstance()

        containerView = findViewById(R.id.container_view)
        progressBar = findViewById(R.id.model_progress_bar)
        infoSaveContainer = findViewById(R.id.info_save_container)
        progressBar!!.visibility = View.GONE

        findViewById<View>(R.id.vr_fab).setOnClickListener { startVrActivity() }

        uri = pathUtils.getObjectURI(presenterObject)
        uri?.let { beginLoadModel(it) }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState?.let {
            outState.putParcelable(PRESENTER_OBJECT, presenterObject)
            outState.putSerializable(CATEGORY_INFO, categoryInfo)
        }
    }

    override fun onStart() {
        super.onStart()
        createNewModelView(app.currentModel)
    }

    override fun onPause() {
        super.onPause()
        modelView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        modelView?.onResume()
    }

    override fun onStop() {
        super.onStop()
        app.currentModel = null
    }

    private fun beginLoadModel(uri: Uri) {
        progressBar.visibility = View.VISIBLE
        ModelLoadTask().execute(uri)
    }

    private fun createNewModelView(model: Model?) {
        modelView?.let { containerView.removeView(it) }
        ModelViewerApplication.getInstance().currentModel = model
        modelView = ModelSurfaceView(this, model)
        containerView.addView(modelView, 0)
    }

    private inner class ModelLoadTask : AsyncTask<Uri, Int, Model>() {
        override fun doInBackground(vararg file: Uri): Model? {
            var stream: InputStream? = null
            try {
                val uri = file[0]
                val cr = applicationContext.contentResolver
                val fileName = getFileName(cr, uri)

                if ("http" == uri.scheme || "https" == uri.scheme) {
                    val client = OkHttpClient()
                    val request = Request.Builder().url(uri.toString()).build()
                    val response = client.newCall(request).execute()

                    // TODO: figure out how to NOT need to read the whole file at once.
                    stream = ByteArrayInputStream(response.body()!!.bytes())
                } else {
                    stream = cr.openInputStream(uri)
                }

                if (stream != null) {
                    val model: Model
                    if (!TextUtils.isEmpty(fileName)) {
                        if (fileName!!.toLowerCase().endsWith(".stl")) {
                            model = StlModel(stream)
                        } else if (fileName.toLowerCase().endsWith(".obj")) {
                            model = ObjModel(stream)
                        } else if (fileName.toLowerCase().endsWith(".ply")) {
                            model = PlyModel(stream)
                        } else {
                            // assume it's STL.
                            model = StlModel(stream)
                        }
                        model.title = fileName
                    } else {
                        // assume it's STL.
                        // TODO: autodetect file type by reading contents?
                        model = StlModel(stream)
                    }
                    return model
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                Util.closeSilently(stream)
            }
            return null
        }

        override fun onProgressUpdate(vararg values: Int?) {}

        override fun onPostExecute(model: Model?) {
            if (isDestroyed) {
                return
            }
            if (model != null) {
                setCurrentModel(model)
            } else {
                Toast.makeText(applicationContext, R.string.open_model_error, Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
        }

        private fun getFileName(cr: ContentResolver, uri: Uri): String? {
            if ("content" == uri.scheme) {
                val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
                val metaCursor = ContentResolverCompat.query(cr, uri, projection, null, null, null, null)
                if (metaCursor != null) {
                    try {
                        if (metaCursor.moveToFirst()) {
                            return metaCursor.getString(0)
                        }
                    } finally {
                        metaCursor.close()
                    }
                }
            }
            return uri.lastPathSegment
        }
    }

    private fun setCurrentModel(model: Model) {
        createNewModelView(model)
        Toast.makeText(applicationContext, R.string.open_model_success, Toast.LENGTH_SHORT).show()
        title = model.title
        progressBar.visibility = View.GONE
    }

    private fun startVrActivity() {
        if (app.currentModel == null) {
            Toast.makeText(this, R.string.view_vr_not_loaded, Toast.LENGTH_SHORT).show()
        } else {
            startActivity(Intent(this, ModelGvrActivity::class.java))
        }
    }

    fun info(view: View) {
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

        featureInfoDialog.show()
    }

    fun save(view: View) {
        uri?.let { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
}

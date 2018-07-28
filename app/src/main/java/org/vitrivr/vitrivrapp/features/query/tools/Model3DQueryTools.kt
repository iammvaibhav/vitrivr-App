package org.vitrivr.vitrivrapp.features.query.tools

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v7.widget.CardView
import android.util.AttributeSet
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.*
import com.dmitrybrant.modelviewer.ModelActivity
import com.nbsp.materialfilepicker.MaterialFilePicker
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.components.drawing.DrawingActivity
import org.vitrivr.vitrivrapp.data.model.enums.QueryTermType
import org.vitrivr.vitrivrapp.features.query.MODEL_CHOOSER_REQUEST_CODE
import org.vitrivr.vitrivrapp.features.query.MODEL_CHOOSER_RESULT
import org.vitrivr.vitrivrapp.features.query.MODEL_DRAWING_RESULT
import org.vitrivr.vitrivrapp.features.query.QueryViewModel
import org.vitrivr.vitrivrapp.utils.checkAndRequestPermission
import java.io.ByteArrayOutputStream
import java.io.File

@SuppressLint("ViewConstructor")
/**
 * query tools for constructing model3d queries
 */
class Model3DQueryTools @JvmOverloads constructor(val queryViewModel: QueryViewModel,
                                                  wasChecked: Boolean,
                                                  toolsContainer: ViewGroup,
                                                  context: Context,
                                                  attrs: AttributeSet? = null,
                                                  defStyleAttr: Int = 0,
                                                  defStyleRes: Int = 0) : View(context, attrs, defStyleAttr, defStyleRes) {

    val loadModel: ImageView
    val resolutionBalance: SeekBar
    val resolutionBalanceContainer: LinearLayout
    val sketchContainer: CardView
    val imagePreview: ImageView
    val loadModelContainer: LinearLayout
    private val sketch2dSwitch: Switch
    val status: TextView

    init {
        // inflate the image_query_tools layout to this view
        LayoutInflater.from(context).inflate(R.layout.model3d_query_tools, toolsContainer, true)

        loadModel = toolsContainer.findViewById(R.id.loadModel)
        resolutionBalance = toolsContainer.findViewById(R.id.resolutionBalance)
        sketchContainer = toolsContainer.findViewById(R.id.sketchContainer)
        imagePreview = toolsContainer.findViewById(R.id.imagePreview)
        sketch2dSwitch = toolsContainer.findViewById(R.id.sketch2d)
        loadModelContainer = toolsContainer.findViewById(R.id.loadModelContainer)
        status = toolsContainer.findViewById(R.id.status)
        resolutionBalanceContainer = toolsContainer.findViewById(R.id.resolutionBalanceContainer)

        loadModel.setOnClickListener {
            (context as Activity).checkAndRequestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, MODEL_CHOOSER_REQUEST_CODE) {
                startModelChooserActivity()
            }
        }

        resolutionBalance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                queryViewModel.setBalance(queryViewModel.currContainerID, QueryTermType.MODEL3D, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sketch2dSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                imagePreview.setImageDrawable(null)
                loadModelContainer.visibility = GONE
                sketchContainer.visibility = VISIBLE
                resolutionBalanceContainer.visibility = GONE
                queryViewModel.putModelUri(queryViewModel.currContainerID, null)
            } else {
                status.text = "No Model Available"
                status.setOnClickListener(null)
                resolutionBalanceContainer.visibility = VISIBLE
                loadModelContainer.visibility = VISIBLE
                sketchContainer.visibility = GONE
                val preview = DrawingActivity.getResultantImageFile(context, queryViewModel.currContainerID, QueryTermType.MODEL3D)
                val orig = DrawingActivity.getOriginalImageFile(context, queryViewModel.currContainerID, QueryTermType.MODEL3D)

                if (preview.exists()) preview.delete()
                if (orig.exists()) orig.delete()
            }
        }

        imagePreview.setOnClickListener {
            val intent = Intent(context, DrawingActivity::class.java)
            intent.putExtra(DrawingActivity.INTENT_EXTRA_CONTAINER_ID, queryViewModel.currContainerID)
            intent.putExtra(DrawingActivity.INTENT_EXTRA_TERM_TYPE, QueryTermType.MODEL3D.name)
            (context as Activity).startActivityForResult(intent, MODEL_DRAWING_RESULT)
        }

        if (wasChecked) {
            restoreState()
        } else {
            queryViewModel.addQueryTermToContainer(queryViewModel.currContainerID, QueryTermType.MODEL3D)
        }
    }

    private fun restoreState() {
        //restore state
        val preview = DrawingActivity.getResultantImageFile(context, queryViewModel.currContainerID, QueryTermType.MODEL3D)
        if (preview.exists()) {
            //2D sketch
            sketch2dSwitch.isChecked = true
            val image = BitmapFactory.decodeFile(preview.absolutePath)
            imagePreview.setImageBitmap(image)
        } else {
            sketch2dSwitch.isChecked = false
            resolutionBalance.progress = queryViewModel.getBalance(queryViewModel.currContainerID, QueryTermType.MODEL3D)
            queryViewModel.getModelUri(queryViewModel.currContainerID)?.let {
                val uri = it
                status.text = context.getString(R.string.model_loaded)
                status.setOnClickListener {
                    val modelIntent = Intent(context, ModelActivity::class.java)
                    modelIntent.data = uri
                    (context as Activity).startActivity(modelIntent)
                }
            }
        }
    }

    /**
     * starts the model chooser activity
     */
    fun startModelChooserActivity() {
        MaterialFilePicker()
                .withActivity(context as Activity)
                .withRequestCode(MODEL_CHOOSER_RESULT)
                .start()
    }

    /**
     * handles chosen model result.
     * WebView and JavaScript interface is required to use three.js javascript library
     */
    fun handleChosenModel(filePath: String) {
        status.text = context.getString(R.string.loading_model)
        val uri = Uri.fromFile(File(filePath))
        val html = getHTMLForModel(uri)

        if (html == null) {
            Toast.makeText(context, "Error! Please choose obj/stl file only", Toast.LENGTH_SHORT).show()
            return
        }

        /**
         * prepare webView
         */
        val webView = WebView(context)
        webView.settings.allowFileAccess = true
        webView.settings.javaScriptEnabled = true
        webView.settings.allowUniversalAccessFromFileURLs = true
        webView.settings.allowFileAccessFromFileURLs = true

        webView.addJavascriptInterface(WebViewJavaScriptInterface {
            (context as Activity).runOnUiThread {
                queryViewModel.putModelUri(queryViewModel.currContainerID, uri)
                queryViewModel.setDataOfQueryTerm(queryViewModel.currContainerID, QueryTermType.MODEL3D, it)

                status.text = context.getText(R.string.model_loaded)
                status.setOnClickListener {
                    val modelIntent = Intent(context, ModelActivity::class.java)
                    modelIntent.data = uri
                    (context as Activity).startActivity(modelIntent)
                }
            }
        }, "app")

        webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html; charset=utf-8", "UTF-8", null)
    }

    /**
     * handles the drawing result
     */
    fun handleDrawingResult() {
        val image = BitmapFactory.decodeFile(DrawingActivity.getResultantImageFile(context, queryViewModel.currContainerID,
                QueryTermType.MODEL3D).absolutePath)

        val outputStream = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        queryViewModel.setDataOfQueryTerm(queryViewModel.currContainerID, QueryTermType.MODEL3D, base64String, 1)
    }

    /**
     * This JavaScript interface is used to get result from three.js library
     */
    @Suppress("unused")
    class WebViewJavaScriptInterface(private val processBase64JSONData: (String) -> Unit) {
        @JavascriptInterface
        fun processModelBase64JSONData(base64json: String) {
            processBase64JSONData(base64json)
        }
    }

    /**
     * returns the HTML used for processing for the uri
     */
    private fun getHTMLForModel(uri: Uri): String? {
        val extension = File(uri.path).extension.toLowerCase()
        return when (extension) {
            "obj" -> getHTMLforOBJ(uri)
            "stl" -> getHTMLforSTL(uri)
            else -> null
        }
    }

    /**
     * return the html used for processing for given uri pointing to valid stl file
     */
    private fun getHTMLforSTL(uri: Uri) = """
                <!DOCTYPE html>
                <html>
                  <head>
                    <script src="three.min.js"></script>
                    <script src="STLLoader.js"></script>
                  </head>
                  <body>
                    <script>
                    var loader =  new THREE.STLLoader();
                        loader.load('$uri', function (geometry) {
                            if (geometry instanceof THREE.BufferGeometry) {
                                geometry = new THREE.Geometry().fromBufferGeometry(geometry);
                            }
                            app.processModelBase64JSONData(btoa(JSON.stringify(geometry.toJSON().data)))
                        });
                    </script>
                  </body>
                </html>
                """

    /**
     * returns the html used for processing for given uri pointing to valid obj file
     */
    private fun getHTMLforOBJ(uri: Uri) = """
                <!DOCTYPE html>
                <html>
                  <head>
                    <script src="three.min.js"></script>
                    <script src="OBJLoader.js"></script>
                  </head>
                  <body>
                    <script>
                    var loader = new THREE.OBJLoader();
                              loader.load('$uri', function(object) {
                                  if (object) {
                                      var geometry = new THREE.Geometry();
                                      object.traverse( function(child) {
                                          if (child instanceof THREE.Mesh) {
                                              child.updateMatrix();
                                              if (child.geometry instanceof THREE.BufferGeometry) {
                                                  var partial = (new THREE.Geometry()).fromBufferGeometry( child.geometry );
                                                  geometry.merge(partial, child.matrix);
                                              } else if (child.geometry instanceof THREE.Geometry) {
                                                  geometry.merge(child.geometry, child.matrix);
                                              }
                                          }
                                      });
                                      app.processModelBase64JSONData(btoa(JSON.stringify(geometry.toJSON().data)))
                                  }
                              });
                    </script>
                  </body>
                </html>
                """
}
package org.vitrivr.vitrivrapp.components.drawing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.view.View
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.motion_drawing_activity.*
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.data.helper.SharedPreferenceHelper
import org.vitrivr.vitrivrapp.data.model.query.Coordinate
import org.vitrivr.vitrivrapp.data.model.query.MotionObject
import org.vitrivr.vitrivrapp.data.model.query.MotionQueryDataModel
import java.io.File
import java.io.FileOutputStream

@Suppress("PropertyName", "PrivatePropertyName", "NAME_SHADOWING", "UNUSED_PARAMETER")
/**
 * This class extends AppCompatImageView to provide motion drawing capabilities on it.
 */
class MotionDrawingActivity : AppCompatActivity() {

    companion object {
        const val INTENT_EXTRA_CONTAINER_ID = "INTENT_EXTRA_CONTAINER_ID"

        fun getResultantMotionImageFile(context: Context, containerID: Long): File {
            return File(context.filesDir, "motionQuery_$containerID.png")
        }
    }

    private var spHelper: SharedPreferenceHelper

    private val PREF_NAME_MOTION_DRAWING_DATA = "PREF_NAME_MOTION_DRAWING_DATA"

    private lateinit var PATH_LIST: String
    private lateinit var ARROW_LIST: String
    private lateinit var PATH_LIST_SAVE: String
    private lateinit var ARROW_LIST_SAVE: String
    private lateinit var resultantImage: String
    lateinit var motionDrawingCanvas: MotionDrawableImageView

    init {
        spHelper = SharedPreferenceHelper(PREF_NAME_MOTION_DRAWING_DATA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.motion_drawing_activity)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        motionDrawingCanvas = findViewById(R.id.motionDrawingCanvas)

        val containerID = intent.getLongExtra(INTENT_EXTRA_CONTAINER_ID, 0)
        PATH_LIST = "PATH_LIST_$containerID"
        ARROW_LIST = "ARROW_LIST_$containerID"
        PATH_LIST_SAVE = "PATH_LIST_SAVE_$containerID"
        ARROW_LIST_SAVE = "ARROW_LIST_SAVE_$containerID"

        resultantImage = "motionQuery_$containerID.png"

        motionDrawingCanvas.post {
            val bitmap = Bitmap.createBitmap(motionDrawingCanvas.width, motionDrawingCanvas.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawARGB(255, 255, 255, 255)
            motionDrawingCanvas.setImageBitmap(bitmap)
        }

        background(null)

        val pathList = when {
            spHelper.hasHey(PATH_LIST) -> spHelper.getString(PATH_LIST)
            spHelper.hasHey(PATH_LIST_SAVE) -> spHelper.getString(PATH_LIST_SAVE)
            else -> null
        }

        val arrowList = when {
            spHelper.hasHey(ARROW_LIST) -> spHelper.getString(ARROW_LIST)
            spHelper.hasHey(ARROW_LIST_SAVE) -> spHelper.getString(ARROW_LIST_SAVE)
            else -> null
        }

        if (pathList != null && arrowList != null) {

            val pathList = Gson().fromJson<ArrayList<Pair<ArrayList<SerializablePath.Action>, Int>>>(pathList,
                    object : TypeToken<ArrayList<Pair<ArrayList<SerializablePath.Action>, Int>>>() {}.type)
                    ?.map {
                        Pair(SerializablePath().apply {
                            //actions = it.first
                            it.first.forEach { it.perform(this) }
                        }, Paint(motionDrawingCanvas.pathPaint).apply { color = it.second })
                    }

            val arrowList = Gson().fromJson<ArrayList<Pair<Arrow, Int>>>(arrowList,
                    object : TypeToken<ArrayList<Pair<Arrow, Int>>>() {}.type)
                    ?.map { Pair(it.first.apply { prepareMatrixFromValues() }, Paint(motionDrawingCanvas.arrowPaint).apply { color = it.second }) }

            if (pathList != null && arrowList != null) {
                motionDrawingCanvas.paths.addAll(pathList)
                motionDrawingCanvas.arrows.addAll(arrowList)
                motionDrawingCanvas.invalidate()
            }
        }
    }

    fun back(view: View) {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    fun save(view: View) {
        val serializablePath = motionDrawingCanvas.paths.map { Pair(it.first.actions, it.second.color) }
        val serializableArrow = motionDrawingCanvas.arrows.map { Pair(it.first, it.second.color) }

        spHelper.putListObject(PATH_LIST_SAVE, serializablePath)
        spHelper.putListObject(ARROW_LIST_SAVE, serializableArrow)

        val dir = filesDir
        val file = File(dir, resultantImage)
        val stream = FileOutputStream(file)
        (motionDrawingCanvas.drawable as BitmapDrawable).bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush()
        stream.close()

        val base64Data = getBase64String(motionDrawingCanvas.paths, motionDrawingCanvas.width, motionDrawingCanvas.height)

        val data = Intent()
        data.putExtra("base64", base64Data)
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    fun clear(view: View) {
        motionDrawingCanvas.clear()
        val file = File(filesDir, resultantImage)
        if (file.exists()) file.delete()
        spHelper.removeKey(PATH_LIST)
        spHelper.removeKey(ARROW_LIST)
        spHelper.removeKey(PATH_LIST_SAVE)
        spHelper.removeKey(ARROW_LIST_SAVE)
    }

    fun background(view: View?) {
        motionDrawingCanvas.selectMotion(MotionDrawableImageView.Companion.MotionDescription.BACKGROUND)
        backgroundIcon.setColorFilter(Color.BLACK)
        backgroundText.setTextColor(Color.BLACK)
        foregroundIcon.setColorFilter(Color.parseColor("#555555"))
        foregroundText.setTextColor(Color.parseColor("#555555"))
    }

    fun foreground(view: View) {
        motionDrawingCanvas.selectMotion(MotionDrawableImageView.Companion.MotionDescription.FOREGROUND)
        foregroundIcon.setColorFilter(Color.BLACK)
        foregroundText.setTextColor(Color.BLACK)
        backgroundIcon.setColorFilter(Color.parseColor("#555555"))
        backgroundText.setTextColor(Color.parseColor("#555555"))
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        val serializablePath = motionDrawingCanvas.paths.map { Pair(it.first.actions, it.second.color) }
        val serializableArrow = motionDrawingCanvas.arrows.map { Pair(it.first, it.second.color) }

        spHelper.putListObject(PATH_LIST, serializablePath)
        spHelper.putListObject(ARROW_LIST, serializableArrow)
    }

    private fun getBase64String(paths: List<Pair<SerializablePath, Paint>>, canvasWidth: Int, canvasHeight: Int): String {
        val model = MotionQueryDataModel(ArrayList(), ArrayList())

        for (pathPair in paths) {
            val pathMeasure = PathMeasure(pathPair.first, false)
            val speed = 10f
            val totalDistance = pathMeasure.length
            var distanceCovered = 0f
            val coordinatesArray = FloatArray(2)
            val type = getTypeFromPaint(pathPair.second)
            val motionObject = MotionObject(ArrayList(), type.name)
            when (type) {
                MotionDrawableImageView.Companion.MotionDescription.FOREGROUND -> model.foreground.add(motionObject)
                MotionDrawableImageView.Companion.MotionDescription.BACKGROUND -> model.background.add(motionObject)
            }

            while (distanceCovered <= totalDistance) {
                pathMeasure.getPosTan(distanceCovered, coordinatesArray, null)
                motionObject.path.add(Coordinate(coordinatesArray[0] / canvasWidth, coordinatesArray[1] / canvasHeight))
                distanceCovered += speed
            }
        }

        return Base64.encodeToString(Gson().toJson(model).toByteArray(), Base64.NO_WRAP)
    }

    override fun onStop() {
        super.onStop()

        if (isFinishing) {
            spHelper.removeKey(PATH_LIST)
            spHelper.removeKey(ARROW_LIST)
        }
    }

    private fun getTypeFromPaint(paint: Paint): MotionDrawableImageView.Companion.MotionDescription {
        when (paint.color) {
            Color.RED -> MotionDrawableImageView.Companion.MotionDescription.FOREGROUND
            Color.GREEN -> MotionDrawableImageView.Companion.MotionDescription.BACKGROUND
        }

        //dummy return
        return MotionDrawableImageView.Companion.MotionDescription.FOREGROUND
    }
}
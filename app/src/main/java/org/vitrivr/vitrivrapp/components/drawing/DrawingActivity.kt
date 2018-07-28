package org.vitrivr.vitrivrapp.components.drawing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.SeekBar
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import com.jaredrummler.android.colorpicker.ColorShape
import kotlinx.android.synthetic.main.drawing_activity.*
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.data.model.enums.QueryTermType
import org.vitrivr.vitrivrapp.utils.px
import org.vitrivr.vitrivrapp.utils.showToast
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

@Suppress("UNUSED_PARAMETER")
class DrawingActivity : AppCompatActivity(), ColorPickerDialogListener {

    companion object {
        const val INTENT_EXTRA_CONTAINER_ID = "INTENT_EXTRA_CONTAINER_ID"
        const val INTENT_EXTRA_TERM_TYPE = "INTENT_EXTRA_TERM_TYPE"
        private const val DIALOG_ID = 1
        private const val SELECT_PHOTO = 1
        private const val TEMPORARY_IMAGE = "TEMPORARY_IMAGE"

        fun getResultantImageFile(context: Context, containerID: Long, termType: QueryTermType): File {
            return File(context.filesDir, "query_${containerID}_$termType.png")
        }

        fun getOriginalImageFile(context: Context, containerID: Long, termType: QueryTermType): File {
            return File(context.filesDir, "query_${containerID}_${termType}_orig.png")
        }
    }

    /**
     * size in pixels of resultant image
     */
    private val pixelWidth = 300

    /**
     * last chosen color
     */
    private var lastColor = Color.parseColor("#555555")

    lateinit var drawingCanvas: DrawableImageView
    lateinit var brushWidth: SeekBar

    /**
     * container ID and term type which started this activity. This will be updated later in onCreate
     */
    private var containerID: Long = 0
    private var termType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.drawing_activity)

        /**
         * setup toolbar
         */
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        drawingCanvas = findViewById(R.id.drawingCanvas)
        brushWidth = findViewById(R.id.brushWidth)

        /**
         * update containerID and termType
         */
        intent.getStringExtra(INTENT_EXTRA_TERM_TYPE)?.let {
            termType = it
            containerID = intent.getLongExtra(INTENT_EXTRA_CONTAINER_ID, 0)
        }

        /**
         * if termType or containerID is not found then set result to cancelled and
         * finish this activity
         */
        if (termType == "" || containerID == 0L) {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        /**
         * temporary image used for restoring work while orientation changes
         */
        val tempFile = File(filesDir, TEMPORARY_IMAGE)
        val origFile = getOriginalImageFile()

        when {
            tempFile.exists() -> {
                val tempImage = BitmapFactory.decodeFile(tempFile.absolutePath)
                drawingCanvas.setImageBitmap(tempImage)
            }
            origFile.exists() -> {
                val origImage = BitmapFactory.decodeFile(origFile.absolutePath)
                drawingCanvas.setImageBitmap(origImage)
            }
            else -> {
                /**
                 * create white bitmap of pixelWidth dp x pixelWidth dp and set to drawing canvas
                 */
                val bitmap = Bitmap.createBitmap(pixelWidth.px, pixelWidth.px, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawARGB(255, 255, 255, 255)
                drawingCanvas.setImageBitmap(bitmap)
            }
        }

        /**
         * setup drawing tools
         */
        drawingCanvas.strokeColor = lastColor
        brushWidth.progressDrawable.setColorFilter(lastColor, PorterDuff.Mode.MULTIPLY)
        brushWidth.thumb.setColorFilter(lastColor, PorterDuff.Mode.SRC_ATOP)
        colorPanel.color = lastColor

        brushWidth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                drawingCanvas.strokeWidth = progress.toFloat()
                val scale = 1 + 0.005f * progress
                colorPanel.scaleX = scale
                colorPanel.scaleY = scale
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    /**
     * Invokes when user presses back button
     * set the result of cancelled and finish the activity
     */
    fun back(view: View) {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    /**
     * Invoked when user presses save button
     * creates resultant image by scaling original image and saves both of them
     */
    fun save(view: View) {
        drawingCanvas.drawable?.let {
            val scaledBitmap = Bitmap.createScaledBitmap((drawingCanvas.drawable as BitmapDrawable).bitmap, pixelWidth, pixelWidth, false)
            val origBitmap = (drawingCanvas.drawable as BitmapDrawable).bitmap

            val stream = FileOutputStream(getResultantImageFile())
            val origStream = FileOutputStream(getOriginalImageFile())

            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            origBitmap.compress(Bitmap.CompressFormat.PNG, 100, origStream)

            stream.flush()
            stream.close()

            origStream.flush()
            origStream.close()

            setResult(Activity.RESULT_OK, null)
            finish()
        }
    }

    /**
     * Invokes when user presses load button
     * starts activity for selecting photos
     */
    fun load(view: View) {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, SELECT_PHOTO)
    }

    /**
     * Invokes when user presses clear button
     * clears the drawing canvas
     */
    fun clear(view: View) {
        drawingCanvas.clear()
        val bitmap = Bitmap.createBitmap(pixelWidth.px, pixelWidth.px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawARGB(255, 255, 255, 255)
        drawingCanvas.setImageBitmap(bitmap)

    }

    /**
     * Invokes when user presses undo button
     * Undo the last action on drawing canvas
     */
    fun undo(view: View) {
        drawingCanvas.undo()
    }

    /**
     * Invokes when user presses fill button
     * Fill the entire canvas with a solid known last color
     */
    fun fill(view: View) {
        drawingCanvas.clear()
        val bitmap = Bitmap.createBitmap(pixelWidth.px, pixelWidth.px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawARGB(Color.alpha(lastColor), Color.red(lastColor), Color.green(lastColor), Color.blue(lastColor))
        drawingCanvas.setImageBitmap(bitmap)
    }

    /**
     * Invokes when user presses select color button
     * shows a color picker dialog for selecting color
     */
    fun selectColor(view: View) {
        ColorPickerDialog.newBuilder()
                .setDialogType(ColorPickerDialog.TYPE_PRESETS)
                .setAllowPresets(true)
                .setColorShape(ColorShape.CIRCLE)
                .setShowAlphaSlider(true)
                .setShowColorShades(true)
                .setAllowCustom(true)
                .setDialogId(DIALOG_ID)
                .setColor(lastColor)
                .show(this)
    }

    override fun onDialogDismissed(dialogId: Int) {
        /* Do nothing */
    }

    /**
     * Invokes when user selects a color from ColorPickerDialog
     */
    override fun onColorSelected(dialogId: Int, color: Int) {
        if (dialogId == DIALOG_ID) {
            colorPanel.color = color
            drawingCanvas.strokeColor = color
            lastColor = color
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            SELECT_PHOTO -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val selectedImage = data.data
                    selectedImage?.let {
                        val bitmap = decodeUri(it)
                        bitmap?.let {
                            drawingCanvas.clear()
                            drawingCanvas.setImageBitmap(it)
                        }
                    }
                } else {
                    "Selection Cancelled".showToast(this)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        /**
         * saves the drawn image for restoring it back
         */
        val tempBitmap = (drawingCanvas.drawable as BitmapDrawable).bitmap
        val tempStream = FileOutputStream(File(filesDir, TEMPORARY_IMAGE))
        tempBitmap.compress(Bitmap.CompressFormat.PNG, 100, tempStream)
        tempStream.flush()
        tempStream.close()
    }

    override fun onStop() {
        super.onStop()

        /**
         * if activity is gonna finish, delete the temporary image if exists
         */
        if (isFinishing) {
            val tempFile = File(filesDir, TEMPORARY_IMAGE)
            if (tempFile.exists())
                tempFile.delete()
        }
    }

    /**
     * @returns original image File object
     */
    private fun getOriginalImageFile(): File {
        return File(filesDir, "query_${containerID}_${termType}_orig.png")
    }

    /**
     * @returns resultant image File object
     */
    private fun getResultantImageFile(): File {
        return File(filesDir, "query_${containerID}_$termType.png")
    }

    @Throws(FileNotFoundException::class)
    /**
     * Decodes the Uri and returns a Bitmap object if decoding is successful
     * @param selectedImage Uri of image to be decoded
     * @returns Bitmap object if decoding is successful else null
     */
    private fun decodeUri(selectedImage: Uri): Bitmap? {

        // Decode image size
        val o = BitmapFactory.Options()
        o.inJustDecodeBounds = true
        BitmapFactory.decodeStream(contentResolver.openInputStream(selectedImage), null, o)

        // The new size we want to scale to
        val requiredSize = (pixelWidth / 2).px

        // Find the correct scale value. It should be the power of 2.
        var widthTmp = o.outWidth
        var heightTmp = o.outHeight
        var scale = 1
        while (true) {
            if (widthTmp / 2 < requiredSize || heightTmp / 2 < requiredSize) {
                break
            }
            widthTmp /= 2
            heightTmp /= 2
            scale *= 2
        }

        // Decode with inSampleSize
        val o2 = BitmapFactory.Options()
        o2.inSampleSize = scale
        return BitmapFactory.decodeStream(contentResolver.openInputStream(selectedImage), null, o2)
    }
}
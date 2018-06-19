package org.vitrivr.vitrivrapp.components.drawing

import android.app.Activity
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
import org.vitrivr.vitrivrapp.utils.px
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

class DrawingActivity : AppCompatActivity(), ColorPickerDialogListener {

    private val DIALOG_ID = 1
    private val SELECT_PHOTO = 1
    private val pixelWidth = 300
    private var lastColor = Color.parseColor("#555555")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.drawing_activity)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        //supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val containerID = intent.getLongExtra("containerID", 0)
        val termType = intent.getStringExtra("termType")

        val origFile = File(filesDir, "imageQuery_image_orig_${containerID}_$termType.png")
        if (origFile.exists()) {
            val origImage = BitmapFactory.decodeFile(origFile.absolutePath)
            drawingCanvas.setImageBitmap(origImage)
        } else {
            val bitmap = Bitmap.createBitmap(pixelWidth.px, pixelWidth.px, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawARGB(255, 255, 255, 255)
            drawingCanvas.setImageBitmap(bitmap)
        }

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

    fun save(view: View) {
        drawingCanvas.drawable?.let {
            val containerID = intent.getLongExtra("containerID", 0)
            val termType = intent.getStringExtra("termType")

            val scaledBitmap = Bitmap.createScaledBitmap((drawingCanvas.drawable as BitmapDrawable).bitmap, pixelWidth, pixelWidth, false)
            val origBitmap = (drawingCanvas.drawable as BitmapDrawable).bitmap

            val dir = filesDir
            val file = File(dir, "imageQuery_image_${containerID}_$termType.png")
            val origFile = File(dir, "imageQuery_image_orig_${containerID}_$termType.png")

            val stream = FileOutputStream(file)
            val origStream = FileOutputStream(origFile)

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

    fun load(view: View) {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, SELECT_PHOTO)
    }

    fun clear(view: View) {
        drawingCanvas.clear()
        val bitmap = Bitmap.createBitmap(pixelWidth.px, pixelWidth.px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawARGB(255, 255, 255, 255)
        drawingCanvas.setImageBitmap(bitmap)

    }

    fun undo(view: View) {
        drawingCanvas.undo()
    }

    fun fill(view: View) {
        drawingCanvas.clear()
        val bitmap = Bitmap.createBitmap(pixelWidth.px, pixelWidth.px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawARGB(Color.alpha(lastColor), Color.red(lastColor), Color.green(lastColor), Color.blue(lastColor))
        drawingCanvas.setImageBitmap(bitmap)
    }

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

    fun back(view: View) {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun onDialogDismissed(dialogId: Int) {}

    override fun onColorSelected(dialogId: Int, color: Int) {
        if (dialogId == DIALOG_ID) {
            colorPanel.color = color
            drawingCanvas.strokeColor = color
            lastColor = color
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_PHOTO && resultCode == RESULT_OK) {
            val selectedImage = data?.data
            selectedImage?.let {
                val bitmap = decodeUri(it)
                bitmap?.let {
                    drawingCanvas.clear()
                    drawingCanvas.setImageBitmap(it)
                }
            }
        }
    }

    @Throws(FileNotFoundException::class)
    private fun decodeUri(selectedImage: Uri): Bitmap? {

        // Decode image size
        val o = BitmapFactory.Options()
        o.inJustDecodeBounds = true
        BitmapFactory.decodeStream(contentResolver.openInputStream(selectedImage), null, o)

        // The new size we want to scale to
        val REQUIRED_SIZE = (pixelWidth / 2).px

        // Find the correct scale value. It should be the power of 2.
        var width_tmp = o.outWidth
        var height_tmp = o.outHeight
        var scale = 1
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE) {
                break
            }
            width_tmp /= 2
            height_tmp /= 2
            scale *= 2
        }

        // Decode with inSampleSize
        val o2 = BitmapFactory.Options()
        o2.inSampleSize = scale
        return BitmapFactory.decodeStream(contentResolver.openInputStream(selectedImage), null, o2)
    }
}
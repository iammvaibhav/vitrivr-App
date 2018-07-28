package org.vitrivr.vitrivrapp.features.query.tools

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AttributeSet
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.components.drawing.DrawingActivity
import org.vitrivr.vitrivrapp.data.model.enums.QueryTermType
import org.vitrivr.vitrivrapp.features.query.DRAWING_RESULT
import org.vitrivr.vitrivrapp.features.query.QueryViewModel
import java.io.ByteArrayOutputStream

@SuppressLint("ViewConstructor")
/**
 * Tools for constructing an image query
 */
class ImageQueryTools @JvmOverloads constructor(val queryViewModel: QueryViewModel,
                                                wasChecked: Boolean,
                                                toolsContainer: ViewGroup,
                                                context: Context,
                                                attrs: AttributeSet? = null,
                                                defStyleAttr: Int = 0,
                                                defStyleRes: Int = 0) : View(context, attrs, defStyleAttr, defStyleRes) {

    val imagePreview: ImageView
    val drawImageBalance: SeekBar

    init {
        /**
         * inflate the image_query_tools layout to this view
         */
        LayoutInflater.from(context).inflate(R.layout.image_query_tools, toolsContainer, true)

        imagePreview = toolsContainer.findViewById(R.id.imagePreview)
        drawImageBalance = toolsContainer.findViewById(R.id.drawImageBalance)

        /**
         * start the drawing activity with containerID and termType as extras for drawing result
         */
        imagePreview.setOnClickListener {
            val intent = Intent(context, DrawingActivity::class.java)
            intent.putExtra(DrawingActivity.INTENT_EXTRA_CONTAINER_ID, queryViewModel.currContainerID)
            intent.putExtra(DrawingActivity.INTENT_EXTRA_TERM_TYPE, QueryTermType.IMAGE.name)
            (context as Activity).startActivityForResult(intent, DRAWING_RESULT)
        }

        /**
         * set the balance in image query
         */
        drawImageBalance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                queryViewModel.setBalance(queryViewModel.currContainerID, QueryTermType.IMAGE, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        /**
         * If it was checked before, implies previous state exists, then restore it
         */
        if (wasChecked) {
            restoreState()
        } else {
            /**
             * else add a new IMAGE query term to query container model
             */
            queryViewModel.addQueryTermToContainer(queryViewModel.currContainerID, QueryTermType.IMAGE)
        }
    }

    /**
     * As a start activity result, DrawingActivity saves the drawn image which is used in this method
     * to convert to base64 string used as an image query data
     */
    fun handleDrawingResult() {
        val image = BitmapFactory.decodeFile(DrawingActivity.getResultantImageFile(context, queryViewModel.currContainerID,
                QueryTermType.IMAGE).absolutePath)

        val outputStream = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        queryViewModel.setDataOfQueryTerm(queryViewModel.currContainerID, QueryTermType.IMAGE, base64String)
    }

    /**
     * restores the previous state
     */
    private fun restoreState() {
        /**
         * restore balance
         */
        drawImageBalance.progress = queryViewModel.getBalance(queryViewModel.currContainerID, QueryTermType.IMAGE)

        /**
         * restore preview image if it exists
         */
        val previewImageFile = DrawingActivity.getResultantImageFile(context, queryViewModel.currContainerID, QueryTermType.IMAGE)
        if (previewImageFile.exists()) {
            val image = BitmapFactory.decodeFile(previewImageFile.absolutePath)
            imagePreview.setImageBitmap(image)
        }
    }

}
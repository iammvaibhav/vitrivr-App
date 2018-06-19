package org.vitrivr.vitrivrapp.features.query.tools

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
import java.io.File

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
        // inflate the image_query_tools layout to this view
        LayoutInflater.from(context).inflate(R.layout.image_query_tools, toolsContainer, true)

        imagePreview = toolsContainer.findViewById(R.id.imagePreview)
        drawImageBalance = toolsContainer.findViewById(R.id.drawImageBalance)

        imagePreview.setOnClickListener {
            val intent = Intent(context, DrawingActivity::class.java)
            intent.putExtra("containerID", queryViewModel.currContainerID)
            intent.putExtra("termType", QueryTermType.IMAGE.name)
            (context as Activity).startActivityForResult(intent, DRAWING_RESULT)
        }

        drawImageBalance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                queryViewModel.setBalance(queryViewModel.currContainerID, QueryTermType.IMAGE, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


        if (wasChecked) {
            restoreState()
        } else {
            queryViewModel.addQueryTermToContainer(queryViewModel.currContainerID, QueryTermType.IMAGE)
        }
    }

    private fun restoreState() {
        drawImageBalance.progress = queryViewModel.getBalance(queryViewModel.currContainerID, QueryTermType.IMAGE)

        val image = BitmapFactory.decodeFile(File((context as Activity).filesDir,
                "imageQuery_image_${queryViewModel.currContainerID}_IMAGE.png").absolutePath)
        imagePreview.setImageBitmap(image)
    }

    fun handleDrawingResult() {
        val image = BitmapFactory.decodeFile(File((context as Activity).filesDir,
                "imageQuery_image_${queryViewModel.currContainerID}_IMAGE.png").absolutePath)

        val outputStream = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        queryViewModel.setDataOfQueryTerm(queryViewModel.currContainerID, QueryTermType.IMAGE, base64String)
    }

}
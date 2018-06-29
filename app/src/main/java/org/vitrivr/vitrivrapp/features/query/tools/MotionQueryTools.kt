package org.vitrivr.vitrivrapp.features.query.tools

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.components.drawing.MotionDrawingActivity
import org.vitrivr.vitrivrapp.data.model.enums.QueryTermType
import org.vitrivr.vitrivrapp.features.query.MOTION_DRAW_RESULT
import org.vitrivr.vitrivrapp.features.query.QueryViewModel
import java.io.File

class MotionQueryTools @JvmOverloads constructor(val queryViewModel: QueryViewModel,
                                                 wasChecked: Boolean,
                                                 toolsContainer: ViewGroup,
                                                 context: Context,
                                                 attrs: AttributeSet? = null,
                                                 defStyleAttr: Int = 0,
                                                 defStyleRes: Int = 0) : View(context, attrs, defStyleAttr, defStyleRes) {

    val imagePreview: ImageView

    init {
        // inflate the image_query_tools layout to this view
        LayoutInflater.from(context).inflate(R.layout.motion_query_tools, toolsContainer, true)

        imagePreview = toolsContainer.findViewById(R.id.imagePreview)

        imagePreview.setOnClickListener {
            val intent = Intent(context, MotionDrawingActivity::class.java)
            intent.putExtra("containerID", queryViewModel.currContainerID)
            (context as Activity).startActivityForResult(intent, MOTION_DRAW_RESULT)
        }

        if (wasChecked) {
            restoreState()
        } else {
            queryViewModel.addQueryTermToContainer(queryViewModel.currContainerID, QueryTermType.MOTION)
        }
    }

    private fun restoreState() {
        val image = BitmapFactory.decodeFile(File((context as Activity).filesDir,
                "MOTION_QUERY_KEY_${queryViewModel.currContainerID}.png").absolutePath)
        imagePreview.setImageBitmap(image)
    }

    fun handleMotionDrawingResult(base64: String) {
        queryViewModel.setDataOfQueryTerm(queryViewModel.currContainerID, QueryTermType.MOTION, base64)
    }

}
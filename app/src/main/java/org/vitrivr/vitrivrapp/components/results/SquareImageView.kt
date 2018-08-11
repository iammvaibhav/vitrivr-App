package org.vitrivr.vitrivrapp.components.results

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView

/**
 * An ImageView with constraint that the height will always be equal to its width
 */
class SquareImageView @JvmOverloads constructor(context: Context,
                                                attrs: AttributeSet? = null,
                                                defStyleAttr: Int = 0,
                                                defStyleRes: Int = 0) :
        ImageView(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = measuredWidth
        setMeasuredDimension(width, width)
    }
}
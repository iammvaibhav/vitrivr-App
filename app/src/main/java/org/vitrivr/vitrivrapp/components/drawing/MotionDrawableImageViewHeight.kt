package org.vitrivr.vitrivrapp.components.drawing

import android.content.Context
import android.util.AttributeSet

class MotionDrawableImageViewHeight @JvmOverloads constructor(context: Context,
                                                              attrs: AttributeSet? = null,
                                                              defStyleAttr: Int = 0,
                                                              defStyleRes: Int = 0) :
        MotionDrawableImageView(context, attrs, defStyleAttr, defStyleRes) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(heightMeasureSpec, heightMeasureSpec)
    }
}

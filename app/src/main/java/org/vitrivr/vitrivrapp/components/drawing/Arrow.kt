package org.vitrivr.vitrivrapp.components.drawing

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PointF
import org.vitrivr.vitrivrapp.utils.px

class Arrow {

    @Transient
    private val point1 = PointF()
    @Transient
    private val point2 = PointF()
    @Transient
    private val point3 = PointF()

    @Transient
    private val path = Path()
    @Transient
    private val matrix = Matrix()

    var modified = false
    var rotation = 0f
    var translationX = 0f
    var translationY = 0f

    init {
        point1.set(0f, -5.px.toFloat())
        point2.set(10.px.toFloat(), 0f)
        point3.set(0f, 5.px.toFloat())
    }

    fun getArrowPath(): Path {
        path.reset()
        path.moveTo(point1.x, point1.y)
        path.lineTo(point2.x, point2.y)
        path.lineTo(point3.x, point3.y)
        path.close()

        path.transform(matrix)
        return path
    }

    fun rotateThenTranslate(dx: Double, dy: Double, toX: Float, toY: Float) {
        matrix.reset()
        val degrees = Math.atan2(dy, dx) * 180f / Math.PI
        matrix.postRotate(degrees.toFloat())
        matrix.postTranslate(toX, toY)

        translationX = toX
        translationY = toY
        rotation = degrees.toFloat()
        modified = true
    }

    fun prepareMatrixFromValues() {
        matrix.reset()
        matrix.postRotate(rotation)
        matrix.postTranslate(translationX, translationY)
    }
}
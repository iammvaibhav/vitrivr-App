package org.vitrivr.vitrivrapp.components.drawing

import android.graphics.Path
import java.util.*

/**
 * This class extends Path to add limited serialization capabilities to be used in MotionDrawingActivity
 */
class SerializablePath : Path() {

    var actions = ArrayList<Action>()

    override fun lineTo(x: Float, y: Float) {
        actions.add(Action(ActionType.LINE, x, y, null, null))
        super.lineTo(x, y)
    }

    override fun moveTo(x: Float, y: Float) {
        actions.add(Action(ActionType.MOVE, x, y, null, null))
        super.moveTo(x, y)
    }

    override fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        actions.add(Action(ActionType.QUAD, x1, y1, x2, y2))
        super.quadTo(x1, y1, x2, y2)
    }

    data class Action(val actionType: ActionType, val x1: Float, val y1: Float, val x2: Float?, val y2: Float?) {
        fun perform(path: Path) {
            when (actionType) {
                ActionType.MOVE -> path.moveTo(x1, y1)
                ActionType.LINE -> path.lineTo(x1, y1)
                ActionType.QUAD -> path.quadTo(x1, y1, x2!!, y2!!)
            }
        }
    }

    enum class ActionType {
        MOVE, LINE, QUAD
    }
}
package org.vitrivr.vitrivrapp.data.model.query

data class MotionQueryDataModel(val foreground: ArrayList<MotionObject>, val background: ArrayList<MotionObject>)
data class MotionObject(val path: ArrayList<Coordinate>, val type: String)
data class Coordinate(val x: Float, val y: Float)
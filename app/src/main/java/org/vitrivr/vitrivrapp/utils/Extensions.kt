package org.vitrivr.vitrivrapp.utils

import android.content.res.Resources
import java.text.DecimalFormat

val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()
val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

fun Double.format(fracDigits: Int): String {
    val df = DecimalFormat()
    df.maximumFractionDigits = fracDigits
    return df.format(this)
}
package org.vitrivr.vitrivrapp.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.support.v4.app.ActivityCompat
import android.widget.Toast
import java.text.DecimalFormat

/**
 * Convert the given px in Int to dp
 */
val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

/**
 * Convert the given dp in Int to px
 */
val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

/**
 * formats the Double object up to factDigits and returns it as a String
 * @param fracDigits Number of digits required after decimal
 * @return formatted double object as String
 */
fun Double.format(fracDigits: Int): String {
    val df = DecimalFormat()
    df.maximumFractionDigits = fracDigits
    return df.format(this)
}

/**
 * Shows a toast with message as given String for short duration
 * @param context Context
 */
fun String.showToast(context: Context) {
    Toast.makeText(context, this, Toast.LENGTH_SHORT).show()
}

/**
 * Checks if the given permission is granted and executes exec block.
 * If permission is not granted, it is requested with the request code provided
 * @param permission The permission to check
 * @param requestCode RequestCode to be used when requesting permission
 * @param exec block to be executed if permission is granted
 */
fun Activity.checkAndRequestPermission(permission: String, requestCode: Int, exec: () -> Unit) {
    if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    } else {
        exec()
    }
}
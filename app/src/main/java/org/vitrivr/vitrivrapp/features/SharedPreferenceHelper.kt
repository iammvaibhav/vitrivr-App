package org.vitrivr.vitrivrapp.features

import android.content.SharedPreferences

class SharedPreferenceHelper(private val preferences: SharedPreferences) {

    fun putString(key: String, value: String) { preferences.edit().putString(key, value).apply() }
    fun getString(key: String) = preferences.getString(key, null)

    fun putInt(key: String, value: Int) { preferences.edit().putInt(key, value).apply() }
    fun getInt(key: String) = preferences.getInt(key, 0)
}
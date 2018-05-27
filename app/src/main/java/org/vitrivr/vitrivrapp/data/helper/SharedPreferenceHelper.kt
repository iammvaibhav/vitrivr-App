package org.vitrivr.vitrivrapp.data.helper

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.vitrivr.vitrivrapp.App
import javax.inject.Inject

class SharedPreferenceHelper(context: Context, prefName: String) {

    @Inject lateinit var gson: Gson
    private val preferences: SharedPreferences

    init {
        App.daggerAppComponent.inject(this)
        preferences = context.getSharedPreferences(prefName, MODE_PRIVATE)
    }

    fun putString(key: String, value: String) { preferences.edit().putString(key, value).apply() }
    fun getString(key: String): String? = preferences.getString(key, null)

    fun putInt(key: String, value: Int) { preferences.edit().putInt(key, value).apply() }
    fun getInt(key: String): Int? = preferences.getInt(key, 0)

    fun putObject(key: String, obj: Any) {
        putString(key, gson.toJson(obj))
    }

    fun <T> getObject(key: String, classOfT: Class<T>) : T? {
        val json = getString(key)
        try {
            return gson.fromJson<T>(json, classOfT)
        } catch (e: JsonSyntaxException) {
            return null
        }
    }
}
package org.vitrivr.vitrivrapp.data.helper

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import org.vitrivr.vitrivrapp.App
import javax.inject.Inject

/**
 * SharedPreferenceHelper class provides the interface to interact with Shared Preferences
 * @param prefName preference name for the underlying SharedPreferences
 */
class SharedPreferenceHelper(prefName: String) {

    /**
     * Injected parameters.
     * gson is used to serialize objects and context is used to obtain SharedPreferences
     */
    @Inject
    lateinit var gson: Gson
    @Inject
    lateinit var context: Context

    private val preferences: SharedPreferences

    init {
        App.daggerAppComponent.inject(this)
        preferences = context.getSharedPreferences(prefName, MODE_PRIVATE)
    }

    /**
     * puts a string value with given key
     * @param key   key to identify given string
     * @param value string value to store
     */
    fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    /**
     * gets a string with given key
     * @param key key of the string
     * @return if the given key exists, returns the value of the key else returns null
     */
    fun getString(key: String): String? {
        return try {
            preferences.getString(key, null)
        } catch (e: ClassCastException) {
            null
        }
    }

    /**
     * puts an object obj with the given key. Internally, it serializes the object to JSON
     * and store it as a string
     * @param key key to identify given object
     * @param obj object to store
     */
    fun putObject(key: String, obj: Any) {
        putString(key, gson.toJson(obj))
    }

    /**
     * gets an object with given key
     * @param key key of the object
     * @return if the given key exists, returns the object with the given key else returns null
     */
    fun <T> getObject(key: String, classOfT: Class<T>): T? {
        return try {
            val json = getString(key)
            gson.fromJson<T>(json, classOfT)
        } catch (e: ClassCastException) {
            null
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    /**
     * puts a list object list with the given key. Internally, it serializes the list to JSON
     * and store it as a string
     * @param key key to identify given list object
     * @param list list object to store
     */
    fun <T> putListObject(key: String, list: List<T>) {
        putString(key, gson.toJson(list))
    }

    /**
     * gets a list object with the given key
     * @param key key of the list object
     * @return if the given key exists, returns the list object with the given key else returns null
     */
    fun <T> getObjectList(key: String): List<T>? {
        return try {
            val json = getString(key)
            gson.fromJson<List<T>>(json, object : TypeToken<List<T>>() {}.type)
        } catch (e: ClassCastException) {
            null
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    /**
     * removes the given key from shared preferences
     * @param key key to remove
     */
    fun removeKey(key: String) {
        preferences.edit().remove(key).apply()
    }

    /**
     * checks if the given key exists in shared preference
     * @param key key to check
     * @return Boolean indicating if key exist
     */
    fun hasHey(key: String) = preferences.contains(key)
}
package org.vitrivr.vitrivrapp.data.services

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import org.vitrivr.vitrivrapp.data.helper.SharedPreferenceHelper
import org.vitrivr.vitrivrapp.data.model.ResourcesModel
import org.vitrivr.vitrivrapp.data.model.ServerModel
import javax.inject.Inject

class SettingsService @Inject constructor(context: Context) {

    val API_SETTINGS_KEY = "API_SETTINGS"
    private val SERVER_SETTINGS_KEY = "SERVER_SETTINGS_KEY"
    private val RESOURCES_SETTINGS_KEY = "RESOURCES_SETTINGS_KEY"
    private val spHelper = SharedPreferenceHelper(context, API_SETTINGS_KEY)

    fun getServerSettings(): LiveData<ServerModel> {
        val serverSettings = MutableLiveData<ServerModel>()
        val serverModel = spHelper.getObject(SERVER_SETTINGS_KEY, ServerModel::class.java)
        serverModel?.let { serverSettings.value = serverModel }
        return serverSettings
    }

    fun getResourcesSettings(): LiveData<ResourcesModel> {
        val resourcesSettings = MutableLiveData<ResourcesModel>()
        val resModel = spHelper.getObject(RESOURCES_SETTINGS_KEY, ResourcesModel::class.java)
        resModel?.let { resourcesSettings.value = resModel }
        return resourcesSettings
    }

    fun saveServerSettings(serverModel: ServerModel) {
        spHelper.putObject(SERVER_SETTINGS_KEY, serverModel)
    }

    fun saveResourcesSettings(resourcesModel: ResourcesModel) {
        spHelper.putObject(RESOURCES_SETTINGS_KEY, resourcesModel)
    }
}
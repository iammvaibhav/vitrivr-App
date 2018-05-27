package org.vitrivr.vitrivrapp.features.settings

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import org.vitrivr.vitrivrapp.data.helper.SharedPreferenceHelper
import org.vitrivr.vitrivrapp.data.model.ResourcesModel
import org.vitrivr.vitrivrapp.data.model.ServerModel

class SettingsViewModel constructor(context: Context, prefName: String): ViewModel() {

    private val SERVER_SETTINGS_KEY = "SERVER_SETTINGS_KEY"
    private val RESOURCES_SETTINGS_KEY = "RESOURCES_SETTINGS_KEY"
    private val spHelper = SharedPreferenceHelper(context, prefName)

    fun getServerSettings() : LiveData<ServerModel> {
        val serverSettings = MutableLiveData<ServerModel>()
        val serverModel = spHelper.getObject(SERVER_SETTINGS_KEY, ServerModel::class.java)
        serverModel?.let { serverSettings.value = serverModel }
        return serverSettings
    }

    fun getResourcesSettings() : LiveData<ResourcesModel> {
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
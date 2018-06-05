package org.vitrivr.vitrivrapp.data.services

import android.content.Context
import org.vitrivr.vitrivrapp.data.helper.SharedPreferenceHelper
import org.vitrivr.vitrivrapp.data.model.settings.ResourcesModel
import org.vitrivr.vitrivrapp.data.model.settings.ServerModel
import javax.inject.Inject

class SettingsService @Inject constructor(context: Context) {

    val API_SETTINGS_KEY = "API_SETTINGS"
    private val SERVER_SETTINGS_KEY = "SERVER_SETTINGS_KEY"
    private val RESOURCES_SETTINGS_KEY = "RESOURCES_SETTINGS_KEY"
    private val spHelper = SharedPreferenceHelper(context, API_SETTINGS_KEY)

    fun getServerSettings(): ServerModel? = spHelper.getObject(SERVER_SETTINGS_KEY, ServerModel::class.java)

    fun getResourcesSettings(): ResourcesModel? = spHelper.getObject(RESOURCES_SETTINGS_KEY, ResourcesModel::class.java)

    fun saveServerSettings(serverModel: ServerModel) {
        spHelper.putObject(SERVER_SETTINGS_KEY, serverModel)
    }

    fun saveResourcesSettings(resourcesModel: ResourcesModel) {
        spHelper.putObject(RESOURCES_SETTINGS_KEY, resourcesModel)
    }
}
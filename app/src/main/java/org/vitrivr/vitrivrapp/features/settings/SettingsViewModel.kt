package org.vitrivr.vitrivrapp.features.settings

import android.arch.lifecycle.ViewModel
import org.vitrivr.vitrivrapp.App
import org.vitrivr.vitrivrapp.data.model.ResourcesModel
import org.vitrivr.vitrivrapp.data.model.ServerModel
import org.vitrivr.vitrivrapp.data.services.SettingsService
import javax.inject.Inject

class SettingsViewModel : ViewModel() {

    @Inject
    lateinit var settingsService: SettingsService

    init {
        App.daggerAppComponent.inject(this)
    }

    fun getServerSettings() = settingsService.getServerSettings()

    fun getResourcesSettings() = settingsService.getResourcesSettings()

    fun saveServerSettings(serverModel: ServerModel) = settingsService.saveServerSettings(serverModel)

    fun saveResourcesSettings(resourcesModel: ResourcesModel) = settingsService.saveResourcesSettings(resourcesModel)
}
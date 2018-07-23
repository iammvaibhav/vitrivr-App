package org.vitrivr.vitrivrapp.features.settings

import android.arch.lifecycle.ViewModel
import org.vitrivr.vitrivrapp.App
import org.vitrivr.vitrivrapp.data.model.settings.CineastAPIModel
import org.vitrivr.vitrivrapp.data.model.settings.ResourcesModel
import org.vitrivr.vitrivrapp.data.services.SettingsService
import javax.inject.Inject

/**
 * ViewModel for SettingsActivity
 */
class SettingsViewModel : ViewModel() {

    @Inject
    lateinit var settingsService: SettingsService

    init {
        App.daggerAppComponent.inject(this)
    }

    /**
     * Delegates the call to SettingsService object
     * @return CineastAPIModel object if exists, else returns null
     */
    fun getCineastAPISettings() = settingsService.getCineastAPISettings()

    /**
     * Delegates the call to SettingsService object
     * @return ResourcesModel object if exists, else returns null
     */
    fun getResourcesSettings() = settingsService.getResourcesSettings()

    /**
     * Delegates the call to SettingsService object
     * save Cineast API Settings
     * @param cineastAPIModel object to save
     */
    fun saveCineastAPISettings(cineastAPIModel: CineastAPIModel) = settingsService.saveCineastAPISettings(cineastAPIModel)

    /**
     * Delegates the call to SettingsService object
     * save Resources Settings
     * @param resourcesModel object to save
     */
    fun saveResourcesSettings(resourcesModel: ResourcesModel) = settingsService.saveResourcesSettings(resourcesModel)
}
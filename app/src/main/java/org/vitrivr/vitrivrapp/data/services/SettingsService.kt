package org.vitrivr.vitrivrapp.data.services

import org.vitrivr.vitrivrapp.data.helper.SharedPreferenceHelper
import org.vitrivr.vitrivrapp.data.model.settings.CineastAPIModel
import org.vitrivr.vitrivrapp.data.model.settings.ResourcesModel

/**
 * This class interacts with the shared preferences to save and get API and Resources related settings
 */
@Suppress("PrivatePropertyName")
class SettingsService {

    private val API_SETTINGS_KEY = "API_SETTINGS_KEY"
    private val SERVER_SETTINGS_KEY = "SERVER_SETTINGS_KEY"
    private val RESOURCES_SETTINGS_KEY = "RESOURCES_SETTINGS_KEY"

    private val spHelper = SharedPreferenceHelper(API_SETTINGS_KEY)

    /**
     * @return CineastAPIModel object if exists, else returns null
     */
    fun getCineastAPISettings(): CineastAPIModel? = spHelper.getObject(SERVER_SETTINGS_KEY, CineastAPIModel::class.java)

    /**
     * @return ResourcesModel object if exists, else returns null
     */
    fun getResourcesSettings(): ResourcesModel? = spHelper.getObject(RESOURCES_SETTINGS_KEY, ResourcesModel::class.java)

    /**
     * save Cineast API Settings
     * @param cineastAPIModel object to save
     */
    fun saveCineastAPISettings(cineastAPIModel: CineastAPIModel) {
        spHelper.putObject(SERVER_SETTINGS_KEY, cineastAPIModel)
    }

    /**
     * save Resources Settings
     * @param resourcesModel object to save
     */
    fun saveResourcesSettings(resourcesModel: ResourcesModel) {
        spHelper.putObject(RESOURCES_SETTINGS_KEY, resourcesModel)
    }

    /**
     * @return websocket endpoint URL for sending queries if Cineast API Settings exists. If not, returns null
     */
    fun getWebSocketEndpointURL(): String? {
        getCineastAPISettings()?.let {
            return "${it.protocol}://${it.address}:${it.port}/api/v1/websocket"
        }
        return null
    }
}
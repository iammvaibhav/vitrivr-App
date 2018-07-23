package org.vitrivr.vitrivrapp.features.settings

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.settings_activity.*
import net.rdrei.android.dirchooser.DirectoryChooserActivity
import net.rdrei.android.dirchooser.DirectoryChooserConfig
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.data.model.settings.CineastAPIModel
import org.vitrivr.vitrivrapp.data.model.settings.ResourcesModel
import org.vitrivr.vitrivrapp.utils.checkAndRequestPermission
import org.vitrivr.vitrivrapp.utils.showToast
import java.net.URI

/**
 * This activity is used to setup Cineast server settings as well as resources settings for
 * thumbnails and media objects.
 *
 * Cineast websocket API is used for querying.
 */
@Suppress("PrivatePropertyName", "UNUSED_PARAMETER")
class SettingsActivity : AppCompatActivity() {

    private val THUMBNAILS_PICK_FOLDER_REQUEST_CODE = 1
    private val OBJECTS_PICK_FOLDER_REQUEST_CODE = 2
    private val THUMBNAILS_READ_PERMISSION_REQUEST_CODE = 1
    private val OBJECTS_READ_PERMISSION_REQUEST_CODE = 2

    private lateinit var settingsViewModel: SettingsViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        /**
         * Setting toolbar
         */
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)

        /**
         * Obtaining ViewModel and attaching it to binding
         */
        settingsViewModel = ViewModelProviders.of(this).get(SettingsViewModel::class.java)

        /**
         * New SettingsActivity
         * if settings exists, restore UI from saved settings
         */
        if (savedInstanceState == null) {
            settingsViewModel.getCineastAPISettings()?.let {
                when (it.protocol) {
                    "ws" -> wsProtocol.isChecked = true
                    "wss" -> wssProtocol.isChecked = true
                }

                serverAddress.setText(it.address)
                serverPort.setText(if (it.port == 0) "" else it.port.toString())
            }

            settingsViewModel.getResourcesSettings()?.let {
                thumbnailsURL.setText(it.thumbnailsURL)
                objectsURL.setText(it.objectsURL)
            }
        }
    }

    /**
     * Validates user entered cineast server settings and shows errors/hints as applicable
     * @return if user entries are valid, returns CineastAPIModel object else returns null
     */
    private fun validateServerSettings(): CineastAPIModel? {
        val socketProtocol = when (protocol.checkedRadioButtonId) {
            wsProtocol.id -> "ws"
            wssProtocol.id -> "wss"
            else -> null
        }

        if (socketProtocol == null) {
            "Please select protocol".showToast(this)
            return null
        }

        if (serverAddress.text.toString().isBlank()) {
            "Please enter server address".showToast(this)
            return null
        }

        if (serverPort.text.toString().toIntOrNull() == null) {
            "Please enter valid port".showToast(this)
            return null
        }

        return CineastAPIModel(socketProtocol, serverAddress.text.toString(), serverPort.text.toString().toInt())
    }

    /**
     * Called when user clicks save button for Cineast API settings.
     * It validates and saves the Cineast API settings.
     * @param view button object
     */
    fun saveCineastSettings(view: View) {
        validateServerSettings()?.let {
            settingsViewModel.saveCineastAPISettings(it)
            "Cineast API Settings Saved".showToast(this)
        }
    }

    /**
     * Validates user entered resources settings and shows errors/hints as applicable
     * @return if user entries are valid, returns ResourcesModel object else returns null
     */
    private fun validateResourcesSettings(): ResourcesModel? {
        if (thumbnailsURL.text.isBlank()) {
            "Please enter thumbnails url/filepath".showToast(this)
            return null
        }

        if (objectsURL.text.isBlank()) {
            "Please enter objects url/filepath".showToast(this)
            return null
        }

        try {
            URI.create(thumbnailsURL.text.toString())
        } catch (e: IllegalArgumentException) {
            "Please enter valid thumbnails url".showToast(this)
            return null
        }

        try {
            URI.create(objectsURL.text.toString())
        } catch (e: IllegalArgumentException) {
            "Please enter valid objects url".showToast(this)
            return null
        }

        return ResourcesModel(thumbnailsURL.text.toString(), objectsURL.text.toString())
    }

    /**
     * Called when user clicks save button for Resources settings.
     * It validates and saves the Resources settings.
     * @param view button object
     */
    fun saveResourcesSettings(view: View) {
        validateResourcesSettings()?.let {
            settingsViewModel.saveResourcesSettings(it)
            "Resources Settings Saved".showToast(this)
        }
    }

    /**
     * @return the intent object which can be used to start DirectoryChooserActivity
     */
    private fun getDirectoryChooserIntent(): Intent {
        val chooserIntent = Intent(this, DirectoryChooserActivity::class.java)

        val config = DirectoryChooserConfig.builder()
                .newDirectoryName("Select Directory")
                .allowReadOnlyDirectory(true)
                .build()

        chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config)
        return chooserIntent
    }

    /**
     * check required permission and start the activity to choose thumbnails folder
     * @param view button object
     */
    fun selectThumbnailsFolder(view: View) {
        checkAndRequestPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE, THUMBNAILS_READ_PERMISSION_REQUEST_CODE) {
            startActivityForResult(getDirectoryChooserIntent(), THUMBNAILS_PICK_FOLDER_REQUEST_CODE)
        }
    }

    /**
     * check required permission and start the activity to choose objects folder
     * @param view button object
     */
    fun selectObjectsFolder(view: View) {
        checkAndRequestPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE, OBJECTS_READ_PERMISSION_REQUEST_CODE) {
            startActivityForResult(getDirectoryChooserIntent(), OBJECTS_PICK_FOLDER_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            THUMBNAILS_PICK_FOLDER_REQUEST_CODE -> if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED && data != null) {
                thumbnailsURL.setText(data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR))
            } else {
                "Selection Cancelled".showToast(this)
            }

            OBJECTS_PICK_FOLDER_REQUEST_CODE -> if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED && data != null) {
                objectsURL.setText(data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR))
            } else {
                "Selection Cancelled".showToast(this)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            THUMBNAILS_READ_PERMISSION_REQUEST_CODE -> {
                if (permissions.isNotEmpty() && permissions[0] == android.Manifest.permission.READ_EXTERNAL_STORAGE &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startActivityForResult(getDirectoryChooserIntent(), THUMBNAILS_PICK_FOLDER_REQUEST_CODE)
                } else {
                    "Storage read permission is required to select directory".showToast(this)
                }
            }

            OBJECTS_READ_PERMISSION_REQUEST_CODE -> {
                if (permissions.isNotEmpty() && permissions[0] == android.Manifest.permission.READ_EXTERNAL_STORAGE &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startActivityForResult(getDirectoryChooserIntent(), OBJECTS_PICK_FOLDER_REQUEST_CODE)
                } else {
                    "Storage read permission is required to select directory".showToast(this)
                }
            }

            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}
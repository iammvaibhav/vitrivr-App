package org.vitrivr.vitrivrapp.features.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.settings_activity.*
import net.rdrei.android.dirchooser.DirectoryChooserActivity
import net.rdrei.android.dirchooser.DirectoryChooserConfig
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.features.SharedPreferenceHelper

class SettingsActivity : AppCompatActivity() {

    val API_SETTINGS_KEY = "API_SETTINGS"
    val CINEAST_ADDR_KEY = "CINEAST_ADDR_KEY"
    val CINEAST_PORT_KEY = "CINEAST_PORT_KEY"
    val THUMBNAILS_URL_KEY = "THUMBNAILS_URL_KEY"
    val OBJECTS_URL_KEY = "OBJECTS_URL_KEY"

    val THUMBNAILS_PICK_FOLDER_REQUEST_CODE = 1
    val OBJECTS_PICK_FOLDER_REQUEST_CODE = 2
    val THUMBNAILS_WRITE_PERMISSION_REQUEST = 1
    val OBJECTS_WRITE_PERMISSION_REQUEST = 1

    lateinit var spHelper: SharedPreferenceHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        spHelper = SharedPreferenceHelper(getSharedPreferences(API_SETTINGS_KEY, Context.MODE_PRIVATE))

        serverAddress.setText(spHelper.getString(CINEAST_ADDR_KEY))
        if (spHelper.getInt(CINEAST_PORT_KEY) != 0)
            serverPort.setText(spHelper.getInt(CINEAST_PORT_KEY).toString())
        thumbnailsURL.setText(spHelper.getString(THUMBNAILS_URL_KEY))
        objectsURL.setText(spHelper.getString(OBJECTS_URL_KEY))

        cineastSettingsSave.setOnClickListener {
            spHelper.putString(CINEAST_ADDR_KEY, serverAddress.text.toString())
            spHelper.putInt(CINEAST_PORT_KEY, serverPort.text.toString().toIntOrNull() ?: 0)
            Toast.makeText(this@SettingsActivity, "Server Settings Saved", Toast.LENGTH_SHORT).show()
        }

        resourcesSettingsSave.setOnClickListener {
            spHelper.putString(THUMBNAILS_URL_KEY, thumbnailsURL.text.toString())
            spHelper.putString(OBJECTS_URL_KEY, objectsURL.text.toString())
            Toast.makeText(this@SettingsActivity, "Resources Settings Saved", Toast.LENGTH_SHORT).show()
        }

        thumbnailsSelectFolder.setOnClickListener {
            val chooserIntent = Intent(this, DirectoryChooserActivity::class.java)

            val config = DirectoryChooserConfig.builder()
                    .newDirectoryName("Choose Directory")
                    .allowReadOnlyDirectory(true)
                    .build()

            chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config)

            if (ContextCompat.checkSelfPermission(this@SettingsActivity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this@SettingsActivity, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), THUMBNAILS_WRITE_PERMISSION_REQUEST)
            } else {
                startActivityForResult(chooserIntent, THUMBNAILS_PICK_FOLDER_REQUEST_CODE)
            }

        }

        objectsSelectFolder.setOnClickListener {
            val chooserIntent = Intent(this, DirectoryChooserActivity::class.java)

            val config = DirectoryChooserConfig.builder()
                    .newDirectoryName("Choose Directory")
                    .allowReadOnlyDirectory(true)
                    .allowNewDirectoryNameModification(true)
                    .build()

            chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config)

            if (ContextCompat.checkSelfPermission(this@SettingsActivity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this@SettingsActivity, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), OBJECTS_WRITE_PERMISSION_REQUEST)
            } else {
                startActivityForResult(chooserIntent, OBJECTS_PICK_FOLDER_REQUEST_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            THUMBNAILS_PICK_FOLDER_REQUEST_CODE -> if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
                thumbnailsURL.setText(data?.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR))
            } else {
                // Nothing selected
                Toast.makeText(this, "Selection Cancelled", Toast.LENGTH_SHORT).show()
            }
            OBJECTS_PICK_FOLDER_REQUEST_CODE -> if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
                objectsURL.setText(data?.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR))
            } else {
                // Nothing selected
                Toast.makeText(this, "Selection Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            THUMBNAILS_WRITE_PERMISSION_REQUEST -> if (permissions.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val chooserIntent = Intent(this, DirectoryChooserActivity::class.java)

                val config = DirectoryChooserConfig.builder()
                        .newDirectoryName("Choose Directory")
                        .allowReadOnlyDirectory(true)
                        .build()

                chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config)
                startActivityForResult(chooserIntent, THUMBNAILS_PICK_FOLDER_REQUEST_CODE)
            } else {
                Toast.makeText(this@SettingsActivity, "Storage Access permission is required to select directory", Toast.LENGTH_SHORT).show()
            }
            OBJECTS_WRITE_PERMISSION_REQUEST -> if (permissions.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val chooserIntent = Intent(this, DirectoryChooserActivity::class.java)

                val config = DirectoryChooserConfig.builder()
                        .newDirectoryName("Choose Directory")
                        .allowReadOnlyDirectory(true)
                        .build()

                chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config)
                startActivityForResult(chooserIntent, OBJECTS_PICK_FOLDER_REQUEST_CODE)
            } else {
                Toast.makeText(this@SettingsActivity, "Storage Access permission is required to select directory", Toast.LENGTH_SHORT).show()
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}
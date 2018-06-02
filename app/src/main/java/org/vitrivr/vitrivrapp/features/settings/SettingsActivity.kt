package org.vitrivr.vitrivrapp.features.settings

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.settings_activity.*
import net.rdrei.android.dirchooser.DirectoryChooserActivity
import net.rdrei.android.dirchooser.DirectoryChooserConfig
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.data.model.ResourcesModel
import org.vitrivr.vitrivrapp.data.model.ServerModel
import org.vitrivr.vitrivrapp.databinding.SettingsActivityBinding

class SettingsActivity : AppCompatActivity() {

    val THUMBNAILS_PICK_FOLDER_REQUEST_CODE = 1
    val OBJECTS_PICK_FOLDER_REQUEST_CODE = 2
    val THUMBNAILS_WRITE_PERMISSION_REQUEST = 1
    val OBJECTS_WRITE_PERMISSION_REQUEST = 1

    lateinit var settingsViewModel: SettingsViewModel
    lateinit var binding: SettingsActivityBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.settings_activity)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)

        settingsViewModel = ViewModelProviders.of(this).get(SettingsViewModel::class.java)
        binding.settingsViewModel = settingsViewModel

        cineastSettingsSave.setOnClickListener {
            settingsViewModel.saveServerSettings(ServerModel(serverAddress.text.toString(), serverPort.text.toString().toIntOrNull() ?: 0))
            Toast.makeText(this@SettingsActivity, "Server Settings Saved", Toast.LENGTH_SHORT).show()
        }

        resourcesSettingsSave.setOnClickListener {
            settingsViewModel.saveResourcesSettings(ResourcesModel(thumbnailsURL.text.toString(), objectsURL.text.toString()))
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
package org.vitrivr.vitrivrapp.features.query.tools

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.gson.Gson
import org.vitrivr.vitrivrapp.R
import org.vitrivr.vitrivrapp.data.model.enums.QueryTermType
import org.vitrivr.vitrivrapp.features.query.CURRENT_LOCATION_REQUEST_CODE
import org.vitrivr.vitrivrapp.features.query.QueryViewModel

class LocationQueryTools @JvmOverloads constructor(val queryViewModel: QueryViewModel,
                                                   wasChecked: Boolean,
                                                   toolsContainer: ViewGroup,
                                                   context: Context,
                                                   val fusedLocationProviderClient: FusedLocationProviderClient,
                                                   attrs: AttributeSet? = null,
                                                   defStyleAttr: Int = 0,
                                                   defStyleRes: Int = 0) : View(context, attrs, defStyleAttr, defStyleRes) {

    val latitude: EditText
    val longitude: EditText
    val currentLocation: Button

    init {
        // inflate the image_query_tools layout to this view
        LayoutInflater.from(context).inflate(R.layout.location_query_tools, toolsContainer, true)

        latitude = toolsContainer.findViewById(R.id.latitude)
        longitude = toolsContainer.findViewById(R.id.longitude)
        currentLocation = toolsContainer.findViewById(R.id.currentLocation)

        latitude.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val locationData = queryViewModel.getLocationQueryData(queryViewModel.currContainerID)
                locationData.latitude = latitude.text.toString().toDoubleOrNull() ?: 0.0
                locationData.longitude = longitude.text.toString().toDoubleOrNull() ?: 0.0
                queryViewModel.setDataOfQueryTerm(queryViewModel.currContainerID, QueryTermType.LOCATION, Gson().toJson(locationData))
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        longitude.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val locationData = queryViewModel.getLocationQueryData(queryViewModel.currContainerID)
                locationData.latitude = latitude.text.toString().toDoubleOrNull() ?: 0.0
                locationData.longitude = longitude.text.toString().toDoubleOrNull() ?: 0.0
                queryViewModel.setDataOfQueryTerm(queryViewModel.currContainerID, QueryTermType.LOCATION, Gson().toJson(locationData))
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        currentLocation.setOnClickListener {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(context as Activity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), CURRENT_LOCATION_REQUEST_CODE)
            } else {
                setCurrentLocation()
            }
        }

        if (wasChecked) {
            restoreState()
        } else {
            queryViewModel.addQueryTermToContainer(queryViewModel.currContainerID, QueryTermType.LOCATION)
        }
    }

    fun setCurrentLocation() {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener {
            latitude.setText("${it.latitude}")
            longitude.setText("${it.longitude}")
        }
    }

    private fun restoreState() {
        val locationData = queryViewModel.getLocationQueryData(queryViewModel.currContainerID)
        latitude.setText(locationData.latitude.toString())
        longitude.setText(locationData.longitude.toString())
    }
}
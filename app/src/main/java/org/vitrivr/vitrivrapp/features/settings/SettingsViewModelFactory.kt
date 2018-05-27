package org.vitrivr.vitrivrapp.features.settings

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.content.Context

class SettingsViewModelFactory(private val context: Context, private val prefName: String) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SettingsViewModel(context, prefName) as T
    }
}
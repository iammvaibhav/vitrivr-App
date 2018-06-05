package org.vitrivr.vitrivrapp.di

import dagger.Component
import org.vitrivr.vitrivrapp.data.helper.SharedPreferenceHelper
import org.vitrivr.vitrivrapp.data.repository.QueryResultsRepository
import org.vitrivr.vitrivrapp.features.query.QueryViewModel
import org.vitrivr.vitrivrapp.features.results.PathUtils
import org.vitrivr.vitrivrapp.features.results.ResultsViewModel
import org.vitrivr.vitrivrapp.features.results.ViewDetailsAdapter
import org.vitrivr.vitrivrapp.features.results.ViewSmallAdapter
import org.vitrivr.vitrivrapp.features.settings.SettingsViewModel
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {

    fun inject(sharedPreferenceHelper: SharedPreferenceHelper)
    fun inject(viewModel: QueryViewModel)
    fun inject(viewModel: SettingsViewModel)
    fun inject(resultsViewModel: ResultsViewModel)
    fun inject(queryResultsRepository: QueryResultsRepository)
    fun inject(pathUtils: PathUtils)
    fun inject(viewSmallAdapter: ViewSmallAdapter)
    fun inject(viewDetailsAdapter: ViewDetailsAdapter)

}
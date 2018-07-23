package org.vitrivr.vitrivrapp.di

import dagger.Component
import org.vitrivr.vitrivrapp.App
import org.vitrivr.vitrivrapp.data.helper.SharedPreferenceHelper
import org.vitrivr.vitrivrapp.data.repository.QueryResultsRepository
import org.vitrivr.vitrivrapp.features.addmedia.AddMediaActivity
import org.vitrivr.vitrivrapp.features.query.QueryViewModel
import org.vitrivr.vitrivrapp.features.resultdetails.AllSegmentsAdapter
import org.vitrivr.vitrivrapp.features.resultdetails.ImageResultDetailActivity
import org.vitrivr.vitrivrapp.features.resultdetails.Model3DResultDetailActivity
import org.vitrivr.vitrivrapp.features.resultdetails.VideoResultDetailActivity
import org.vitrivr.vitrivrapp.features.results.PathUtils
import org.vitrivr.vitrivrapp.features.results.ResultsViewModel
import org.vitrivr.vitrivrapp.features.results.ViewDetailsAdapter
import org.vitrivr.vitrivrapp.features.results.ViewSmallAdapter
import org.vitrivr.vitrivrapp.features.settings.SettingsViewModel
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {

    fun inject(app: App)
    fun inject(sharedPreferenceHelper: SharedPreferenceHelper)
    fun inject(viewModel: QueryViewModel)
    fun inject(viewModel: SettingsViewModel)
    fun inject(resultsViewModel: ResultsViewModel)
    fun inject(queryResultsRepository: QueryResultsRepository)
    fun inject(pathUtils: PathUtils)
    fun inject(viewSmallAdapter: ViewSmallAdapter)
    fun inject(viewDetailsAdapter: ViewDetailsAdapter)
    fun inject(imageResultDetailActivity: ImageResultDetailActivity)
    fun inject(allSegmentsAdapter: AllSegmentsAdapter)
    fun inject(videoResultDetailActivity: VideoResultDetailActivity)
    fun inject(model3DResultDetailActivity: Model3DResultDetailActivity)
    fun inject(addMediaActivity: AddMediaActivity)

}
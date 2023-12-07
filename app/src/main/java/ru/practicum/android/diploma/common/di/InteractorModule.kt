package ru.practicum.android.diploma.common.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import ru.practicum.android.diploma.search.data.network.HHSearchRepository
import ru.practicum.android.diploma.search.domain.api.SearchInteractor
import ru.practicum.android.diploma.search.domain.api.SearchResultConverter
import ru.practicum.android.diploma.search.domain.impl.SearchInteractorImpl

@Module
@InstallIn(ViewModelComponent::class)
class InteractorModule {
    @Provides
    fun providesSearchInteractor(
        repository: HHSearchRepository,
        converter: SearchResultConverter
    ): SearchInteractor = SearchInteractorImpl(repository, converter)
}
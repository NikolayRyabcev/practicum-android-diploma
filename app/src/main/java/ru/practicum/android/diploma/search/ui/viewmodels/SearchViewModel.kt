package ru.practicum.android.diploma.search.ui.viewmodels

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.practicum.android.diploma.common.data.dto.Resource
import ru.practicum.android.diploma.common.domain.models.NetworkErrors
import ru.practicum.android.diploma.common.ui.BaseViewModel
import ru.practicum.android.diploma.common.utils.debounce
import ru.practicum.android.diploma.search.domain.api.SearchInteractor
import ru.practicum.android.diploma.search.domain.models.VacanciesSearchResult
import ru.practicum.android.diploma.search.domain.models.VacancyGeneral
import ru.practicum.android.diploma.search.ui.viewmodels.states.ErrorsSearchScreenStates
import ru.practicum.android.diploma.search.ui.viewmodels.states.SearchScreenState
import ru.practicum.android.diploma.search.ui.viewmodels.states.SearchSettingsState
import ru.practicum.android.diploma.search.ui.viewmodels.states.ViewModelInteractionState
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val interactor: SearchInteractor
) : BaseViewModel() {

    companion object {
        private const val SEARCH_DEBOUNCE_DELAY = 2000L
    }

    private var searchSettings: SearchSettingsState = SearchSettingsState()

    // todo кажется можно не хранить их тут — так как вакансии будут в стейте
    private var vacancies: MutableList<VacancyGeneral> = mutableListOf()

    private var _state = MutableStateFlow<SearchScreenState>(SearchScreenState.Error(ErrorsSearchScreenStates.EMPTY))
    val state: StateFlow<SearchScreenState>
        get() = _state

    private val searchDebounce = debounce<SearchSettingsState>(
        SEARCH_DEBOUNCE_DELAY,
        viewModelScope,
        true
    ) {
        getVacancies(it)
    }

    init {
        checkState()
    }

    private fun checkState() {
        if (searchSettings.currentQuery.isEmpty()) {
            _state.value = SearchScreenState.Default
        }
    }

    private fun chargeInteractorSearch(): suspend () -> Flow<Resource<VacanciesSearchResult>> = {
        interactor.searchVacancies(
            text = searchSettings.currentQuery,
            page = searchSettings.currentPage,
            area = searchSettings.currentRegion,
            industry = searchSettings.currentIndustry,
            salary = searchSettings.currentSalary,
            onlyWithSalary = searchSettings.currentSalaryOnly
        )
    }

    private fun handleSearchResponse(result: Resource<VacanciesSearchResult>) {
        when (result) {
            is Resource.Success -> {
                if (result.data?.vacancies.isNullOrEmpty()) {
                    _state.value = SearchScreenState.Error(ErrorsSearchScreenStates.NOT_FOUND)
                } else if (result.data!!.vacanciesFound > 0) {
                    _state.value = SearchScreenState.Content(
                        result.data.totalPages,
                        result.data.currentPage,
                        result.data.vacanciesFound,
                        result.data.vacancies.toList()
                    )
                }
            }

            is Resource.Error -> {
                _state.value = SearchScreenState.Error(
                    when (result.error) {
                        NetworkErrors.NoInternet -> ErrorsSearchScreenStates.NO_INTERNET
                        else -> ErrorsSearchScreenStates.SERVER_ERROR
                    }
                )
            }
        }
    }

    private fun getVacancies(settings: SearchSettingsState) {
        if (settings.currentQuery.isNotEmpty()) {
            _state.value = SearchScreenState.Loading
            viewModelScope.launch {
                chargeInteractorSearch().invoke()
                    .collect { result ->
                        handleSearchResponse(result)
                        _state.emit(_state.value)
                    }
            }
        }
    }

    private fun handleSearchSettings(searchSettings: SearchSettingsState) {
        if (searchSettings.currentQuery.isEmpty()) {
            _state.value = SearchScreenState.Default
        }

        if (searchSettings.currentPage > 0) {
            getVacancies(searchSettings)
        } else {
            vacancies.clear()
            searchDebounce(searchSettings)
        }
    }

    fun handleInteraction(interaction: ViewModelInteractionState) {
        when (interaction) {
            is ViewModelInteractionState.setRegion ->
                searchSettings = searchSettings.copy(currentRegion = interaction.region)

            is ViewModelInteractionState.setIndustry ->
                searchSettings = searchSettings.copy(currentIndustry = interaction.industry)

            is ViewModelInteractionState.setSalary -> searchSettings =
                searchSettings.copy(currentSalary = interaction.salary)

            is ViewModelInteractionState.setSalaryOnly -> searchSettings =
                searchSettings.copy(currentSalaryOnly = interaction.salaryOnly)

            is ViewModelInteractionState.setQuery -> searchSettings =
                searchSettings.copy(currentQuery = interaction.query)

            is ViewModelInteractionState.setPage -> searchSettings =
                searchSettings.copy(currentPage = interaction.page)
        }
        handleSearchSettings(searchSettings)
    }
}

package com.autodrive.app.feature.competition.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.feature.competition.domain.usecase.ObserveWeeklyCompetitionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class WeeklyCompetitionViewModel @Inject constructor(
    private val observeWeeklyCompetition: ObserveWeeklyCompetitionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyCompetitionUiState())
    val uiState = _uiState.asStateFlow()

    private var activeEntryHandled = false
    private var refreshAttempted = false

    init {
        viewModelScope.launch {
            observeWeeklyCompetition().collect { data ->
                val shouldExpose = data.entries.isNotEmpty() || data.isFromCache || refreshAttempted
                if (shouldExpose) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            data = data,
                        )
                    }
                }
            }
        }
    }

    fun onActiveEntry() {
        if (activeEntryHandled) return
        activeEntryHandled = true
        refresh(initial = true)
    }

    fun refresh() = refresh(initial = false)

    private fun refresh(initial: Boolean) {
        viewModelScope.launch {
            val hasData = _uiState.value.data != null
            _uiState.update {
                it.copy(
                    isLoading = initial && !hasData,
                    isRefreshing = !initial || hasData,
                    errorMessage = null,
                )
            }

            runCatching { observeWeeklyCompetition.refresh() }
                .onSuccess {
                    refreshAttempted = true
                    val latest = observeWeeklyCompetition().first()
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isRefreshing = false,
                            data = latest,
                        )
                    }
                }
                .onFailure { error ->
                    refreshAttempted = true
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = error.message ?: "تعذر تحديث ترتيب المسابقة",
                        )
                    }
                }
        }
    }
}

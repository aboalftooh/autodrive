package com.autodrive.app.feature.auth.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.feature.auth.domain.usecase.RestoreSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val restoreSession: RestoreSessionUseCase,
    private val sessionReader: SessionReader
) : ViewModel() {

    private val _startDest = MutableStateFlow<SplashDestination?>(null)
    val startDest: StateFlow<SplashDestination?> = _startDest

    init {
        viewModelScope.launch {
            val hasSession = restoreSession()
            _startDest.value = when {
                !hasSession -> SplashDestination.PHONE_INPUT
                sessionReader.currentSession().isRegistrationComplete -> SplashDestination.HOME
                else -> SplashDestination.REGISTRATION
            }
        }
    }
}


enum class SplashDestination { PHONE_INPUT, HOME, REGISTRATION }

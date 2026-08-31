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
            val cachedBeforeRestore = sessionReader.currentSession()
            val hasSession = restoreSession()
            val current = sessionReader.currentSession()
            _startDest.value = when {
                hasSession && current.isRegistrationComplete -> SplashDestination.HOME
                hasSession -> SplashDestination.REGISTRATION
                !cachedBeforeRestore.pendingJoinRequestId.isNullOrBlank() &&
                    !cachedBeforeRestore.phone.isNullOrBlank() -> SplashDestination.WAITING
                else -> SplashDestination.PHONE_INPUT
            }
        }
    }
}

enum class SplashDestination { PHONE_INPUT, WAITING, HOME, REGISTRATION }

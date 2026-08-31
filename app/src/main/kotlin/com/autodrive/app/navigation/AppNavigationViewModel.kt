package com.autodrive.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.feature.competition.domain.model.CompetitionAvailability
import com.autodrive.app.feature.competition.domain.usecase.ObserveCompetitionAvailabilityUseCase
import com.autodrive.app.feature.notifications.data.UnreadMessagesObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppNavigationViewModel @Inject constructor(
    private val sessionReader: SessionReader,
    private val unreadMessagesObserver: UnreadMessagesObserver,
    private val observeCompetitionAvailability: ObserveCompetitionAvailabilityUseCase,
) : ViewModel() {

    val competitionAvailability: StateFlow<CompetitionAvailability> =
        observeCompetitionAvailability()
            .stateIn(viewModelScope, SharingStarted.Eagerly, CompetitionAvailability.DISABLED)

    init {
        refreshCompetitionAvailability()
    }

    fun observeUnreadMessages(): Flow<Int> = unreadMessagesObserver.observe()

    fun refreshCompetitionAvailability() {
        viewModelScope.launch {
            try {
                observeCompetitionAvailability.refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // The repository preserves a valid cache and DISABLED is the safe default.
            }
        }
    }

    val userName: String
        get() = sessionReader.currentSession().userName.orEmpty()

    val accountType: String
        get() = sessionReader.currentSession().accountType.orEmpty()

    val isRegistrationComplete: Boolean
        get() = sessionReader.currentSession().isRegistrationComplete
}

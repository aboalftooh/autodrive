package com.autodrive.app.feature.achievements.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.core.common.format.FormatUtils
import com.autodrive.app.core.model.money.Money
import com.autodrive.app.core.network.WeeklyPerformanceApi
import com.autodrive.app.core.network.WeeklyPerformanceDto
import com.autodrive.app.core.session.domain.DashboardPreferences
import com.autodrive.app.core.sync.domain.SyncCoordinator
import com.autodrive.app.core.sync.domain.SyncReason
import com.autodrive.app.feature.balance.domain.WithdrawalPolicy
import com.autodrive.app.feature.balance.domain.usecase.ObserveBalanceUseCase
import com.autodrive.app.feature.balance.domain.usecase.ObserveWithdrawalRequestsUseCase
import com.autodrive.app.feature.commission.domain.model.CommissionSnapshotSource
import com.autodrive.app.feature.commission.domain.usecase.ObserveCommissionsUseCase
import com.autodrive.app.feature.profile.domain.usecase.ObserveProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.OffsetDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    observeCommissions: ObserveCommissionsUseCase,
    observeBalance: ObserveBalanceUseCase,
    observeWithdrawals: ObserveWithdrawalRequestsUseCase,
    observeProfile: ObserveProfileUseCase,
    private val withdrawalPolicy: WithdrawalPolicy,
    private val syncCoordinator: SyncCoordinator,
    private val weeklyPerformanceApi: WeeklyPerformanceApi,
    private val dashboardPreferences: DashboardPreferences,
) : ViewModel() {

    private val performance = MutableStateFlow(
        WeeklyPerformanceUiState(
            isRefreshing = true,
            weeklyTarget = dashboardPreferences.weeklyTarget,
            remainingToTarget = dashboardPreferences.weeklyTarget,
        )
    )
    private val editor = MutableStateFlow(TargetEditorState())

    private val financialState = combine(
        observeCommissions(),
        observeBalance(),
        observeWithdrawals(),
        observeProfile(),
    ) { commissions, balance, withdrawals, profile ->
        val hasVerifiedCommissions = commissions.source == CommissionSnapshotSource.SERVER_CACHE
        val hasVerifiedBalance = balance.updatedAt.isNotBlank()
        val summary = commissions.summary
        val fallbackTarget = dashboardPreferences.weeklyTarget

        AchievementsUiState(
            isLoading = !hasVerifiedCommissions || profile == null,
            hasVerifiedCommissions = hasVerifiedCommissions,
            hasVerifiedBalance = hasVerifiedBalance,
            lifetimeCommission = Money.sum(
                listOf(summary.withdrawable, summary.pending, summary.paid),
            ),
            availableBalance = balance.balance,
            pendingCommission = summary.pending,
            joinedAtLabel = profile?.createdAt
                ?.let(FormatUtils::formatJoinDateShort)
                .orEmpty(),
            hasActiveWithdrawal = withdrawalPolicy.activeRequest(withdrawals) != null,
            weeklyPerformance = WeeklyPerformanceUiState(
                hasServerSnapshot = false,
                isRefreshing = true,
                weekStartMs = summary.weekStartMs,
                weekEndMs = summary.weekStartMs.takeIf { it > 0L }
                    ?.plus(SEVEN_DAYS_MS)
                    ?: 0L,
                currentAmount = summary.weeklyTotal,
                weeklyTarget = fallbackTarget,
                remainingToTarget = if (summary.weeklyTotal < fallbackTarget) {
                    fallbackTarget - summary.weeklyTotal
                } else {
                    Money.ZERO
                },
            ),
        )
    }

    val state = combine(financialState, performance, editor) { financial, serverPerformance, editorState ->
        val effectivePerformance = if (serverPerformance.hasServerSnapshot) {
            serverPerformance
        } else {
            financial.weeklyPerformance.copy(
                isRefreshing = serverPerformance.isRefreshing,
                weeklyTarget = serverPerformance.weeklyTarget,
                remainingToTarget = if (financial.weeklyPerformance.currentAmount < serverPerformance.weeklyTarget) {
                    serverPerformance.weeklyTarget - financial.weeklyPerformance.currentAmount
                } else {
                    Money.ZERO
                },
                loadError = serverPerformance.loadError,
            )
        }

        financial.copy(
            weeklyPerformance = effectivePerformance,
            showTargetEditor = editorState.visible,
            targetEditorInitialValue = editorState.initialValue,
            isSavingTarget = editorState.isSaving,
            targetEditorError = editorState.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AchievementsUiState(
            weeklyPerformance = WeeklyPerformanceUiState(
                isRefreshing = true,
                weeklyTarget = dashboardPreferences.weeklyTarget,
                remainingToTarget = dashboardPreferences.weeklyTarget,
            )
        ),
    )

    init {
        viewModelScope.launch {
            runCatching { syncCoordinator.requestSync(SyncReason.USER_REFRESH) }
            refreshPerformanceInternal()
        }
    }

    fun refreshPerformance() {
        viewModelScope.launch { refreshPerformanceInternal() }
    }

    fun openTargetEditor(prefill: Money? = null) {
        val target = prefill ?: state.value.weeklyPerformance.weeklyTarget
        editor.value = TargetEditorState(visible = true, initialValue = target)
    }

    fun dismissTargetEditor() {
        if (editor.value.isSaving) return
        editor.value = TargetEditorState()
    }

    fun saveWeeklyTarget(target: Money) {
        val min = Money.of(100_000L)
        val max = Money.of(5_000_000L)
        if (target < min || target > max) {
            editor.update { it.copy(error = "الهدف يجب أن يكون بين 100,000 و5,000,000") }
            return
        }

        viewModelScope.launch {
            editor.update { it.copy(isSaving = true, error = null) }
            runCatching { weeklyPerformanceApi.setWeeklyTarget(target.amount) }
                .onSuccess { update ->
                    val savedTarget = Money.of(update.weeklyTarget)
                    dashboardPreferences.weeklyTarget = savedTarget
                    performance.update {
                        it.copy(
                            weeklyTarget = savedTarget,
                            loadError = false,
                        )
                    }
                    editor.value = TargetEditorState()
                    refreshPerformanceInternal()
                }
                .onFailure {
                    editor.update {
                        it.copy(
                            isSaving = false,
                            error = "تعذّر حفظ الهدف. تحقق من الاتصال وحاول مرة أخرى.",
                        )
                    }
                }
        }
    }

    fun snoozeTargetSuggestion() {
        viewModelScope.launch {
            runCatching { weeklyPerformanceApi.snoozeTargetSuggestion() }
                .onSuccess {
                    performance.update { it.copy(targetSuggestionVisible = false) }
                }
        }
    }

    private suspend fun refreshPerformanceInternal() {
        performance.update { it.copy(isRefreshing = true) }
        runCatching { weeklyPerformanceApi.getSnapshot() }
            .onSuccess { dto ->
                val ui = dto.toUiState()
                dashboardPreferences.weeklyTarget = ui.weeklyTarget
                performance.value = ui
            }
            .onFailure {
                performance.update {
                    it.copy(
                        isRefreshing = false,
                        loadError = true,
                    )
                }
            }
    }

    private fun WeeklyPerformanceDto.toUiState() = WeeklyPerformanceUiState(
        hasServerSnapshot = true,
        isRefreshing = false,
        weekStartMs = weekStart.toEpochMs(),
        weekEndMs = weekEnd.toEpochMs(),
        currentAmount = Money.of(currentAmount),
        currentCount = currentCount,
        previousSamePeriodAmount = Money.of(previousSamePeriodAmount),
        previousSamePeriodCount = previousSamePeriodCount,
        changePercent = changePercent,
        trend = runCatching { WeeklyPerformanceTrend.valueOf(trend) }
            .getOrDefault(WeeklyPerformanceTrend.NO_BASELINE),
        weeklyTarget = Money.of(weeklyTarget),
        progressPercent = progressPercent,
        remainingToTarget = Money.of(remainingToTarget),
        daysRemaining = daysRemaining,
        requiredDailyAverage = Money.of(requiredDailyAverage),
        targetAchieved = targetAchieved,
        targetAchievedEarly = targetAchievedEarly,
        targetSuggestionVisible = targetSuggestionVisible,
        suggestedTarget = suggestedTarget?.let(Money::of),
        loadError = false,
    )

    private fun String.toEpochMs(): Long = runCatching {
        OffsetDateTime.parse(this).toInstant().toEpochMilli()
    }.recoverCatching {
        Instant.parse(this).toEpochMilli()
    }.getOrDefault(0L)

    private data class TargetEditorState(
        val visible: Boolean = false,
        val initialValue: Money? = null,
        val isSaving: Boolean = false,
        val error: String? = null,
    )

    private companion object {
        const val SEVEN_DAYS_MS = 7L * 24L * 60L * 60L * 1000L
    }
}

package com.autodrive.app.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.core.sync.domain.SyncCoordinator
import com.autodrive.app.core.sync.domain.SyncReason
import com.autodrive.app.feature.commission.domain.model.CommissionSummary
import com.autodrive.app.feature.home.domain.repository.AiInsightRepository
import com.autodrive.app.feature.home.domain.repository.DynamoContentRepository
import com.autodrive.app.feature.competition.domain.repository.WeeklyCompetitionRepository
import com.autodrive.app.feature.balance.domain.usecase.ObserveBalanceUseCase
import com.autodrive.app.feature.commission.domain.usecase.ObserveCommissionsUseCase
import com.autodrive.app.feature.notifications.domain.usecase.ObserveNotificationsUseCase
import com.autodrive.app.feature.notifications.domain.repository.NotificationRepository
import com.autodrive.app.feature.commission.domain.CommissionCalculator
import com.autodrive.app.feature.home.presentation.audio.BenzineSound
import com.autodrive.app.core.session.domain.DashboardPreferences
import com.autodrive.app.core.model.money.Money
import com.autodrive.app.core.network.WeeklyPerformanceApi
import com.autodrive.app.core.session.domain.SessionReader
import dagger.hilt.android.lifecycle.HiltViewModel
import com.autodrive.app.core.observability.AppLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "HomeViewModel"

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeCommissions: ObserveCommissionsUseCase,
    private val observeNotifications: ObserveNotificationsUseCase,
    private val notificationRepository: NotificationRepository,
    private val observeBalance: ObserveBalanceUseCase,
    private val syncCoordinator: SyncCoordinator,
    private val sessionReader: SessionReader,
    private val dashboardPreferences: DashboardPreferences,
    private val calculator: CommissionCalculator,
    private val aiInsightRepository: AiInsightRepository,
    private val weeklyCompetitionRepository: WeeklyCompetitionRepository,
    private val dynamoContentRepository: DynamoContentRepository,
    private val weeklyPerformanceApi: WeeklyPerformanceApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var rotationJob: Job? = null   // AI Insight rotation

    // ─── نصيحة اليوم ───────────────────────────────
    private var lastMessageChangedAt = 0L
    private val recentDynamoIds      = ArrayDeque<String>()

    private companion object {
        const val ROTATION_INTERVAL_MS = 30_000L   // تغيير الرسالة كل 30 ثانية
        const val RESUME_COOLDOWN_MS   = 30_000L   // cooldown عند العودة للشاشة
        const val MAX_RECENT_IDS       = 10
        const val FALLBACK_MSG         = "جاري تحميل النصائح..."
    }

    init {
        resetDisplayedTotalForNewWeek()
        val session = sessionReader.currentSession()
        _uiState.update {
            it.copy(
                nextFriday9AmMs = calculator.fallbackNextFriday9AM(), // fallback — يُحدَّث من summary
                weeklyTarget = dashboardPreferences.weeklyTarget,
                displayedTotal = dashboardPreferences.lastDisplayedTotal,
                userName = session.userName.orEmpty()
            )
        }

        // 1. تشغيل التحديث التلقائي كل 60 ثانية (من Room فقط)
        startDynamoRotation()

        // 2. عرض رسالة فورية من Room ثم sync من Supabase في الخلفية
        viewModelScope.launch {
            pickAndShowMessage()              // من Room (سريع، حتى لو فارغ)
            syncDynamoFromSupabase()          // شبكة في الخلفية
            pickAndShowMessage()              // تحديث بعد الـ sync
        }

        viewModelScope.launch { refreshWeeklyTarget() }
        observeCommissionsData()
        observeBalanceData()
        observeUnreadCount()
        syncNotificationsNow()
        loadInsight()
    }

    @Suppress("DEPRECATION")
    private fun resetDisplayedTotalForNewWeek() {
        val currentWeekStart = calculator.fallbackLastFriday9AM()
        if (dashboardPreferences.lastDisplayedWeekStartMs != currentWeekStart) {
            dashboardPreferences.lastDisplayedWeekStartMs = currentWeekStart
            dashboardPreferences.lastDisplayedTotal = Money.ZERO
        }
    }

    // ─── نصيحة اليوم: اختيار رسالة من Room ────────

    private suspend fun pickAndShowMessage() {
        try {
            val ids = recentDynamoIds.toList()
            val msg = if (ids.isNotEmpty()) {
                dynamoContentRepository.getRandomLocalMessageExcluding(ids)
                    ?: dynamoContentRepository.getRandomLocalMessage()
            } else {
                dynamoContentRepository.getRandomLocalMessage()
            }

            if (msg != null) {
                addToRecentIds(msg.id)
                _uiState.update { it.copy(dynamoMessage = msg.message) }
                lastMessageChangedAt = System.currentTimeMillis()
                AppLogger.d(TAG,"dynamo message shown: ${msg.contentType}")
            } else if (_uiState.value.dynamoMessage.isBlank()) {
                _uiState.update { it.copy(dynamoMessage = FALLBACK_MSG) }
                AppLogger.d(TAG,"dynamo fallback: Room empty, showing placeholder")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG,"pickAndShowMessage error: ${e.message}", e)
        }
    }

    private suspend fun syncDynamoFromSupabase() {
        try {
            val audienceType = resolveAudienceType()
            AppLogger.d(TAG,"syncing dynamo from Supabase (audience=$audienceType)")
            dynamoContentRepository.syncMessages(audienceType, "general")
        } catch (e: Exception) {
            AppLogger.e(TAG,"syncDynamoFromSupabase error: ${e.message}", e)
        }
    }

    // تحديث تلقائي كل 60 ثانية من Room فقط — لا شبكة
    private fun startDynamoRotation() {
        viewModelScope.launch {
            while (isActive) {
                delay(ROTATION_INTERVAL_MS)
                pickAndShowMessage()
                AppLogger.d(TAG,"auto-rotation: message updated")
            }
        }
    }

    // يُستدعى عند العودة للشاشة بعد cooldown (ON_RESUME)
    fun refreshDynamoMessage() {
        val now = System.currentTimeMillis()
        if (now - lastMessageChangedAt < RESUME_COOLDOWN_MS) return
        viewModelScope.launch {
            pickAndShowMessage()
        }
    }

    private fun addToRecentIds(id: String) {
        recentDynamoIds.addFirst(id)
        while (recentDynamoIds.size > MAX_RECENT_IDS) recentDynamoIds.removeLast()
    }

    private fun resolveAudienceType(): String {
        val raw = sessionReader.currentSession().accountType?.lowercase() ?: return "workshop"
        return if (raw == "marketer") "marketer" else "workshop"
    }

    // ─── Data Observation ──────────────────────────

    private fun observeCommissionsData() {
        viewModelScope.launch {
            try {
                observeCommissions().collect { (summary, _) ->
                    applySummary(summary)
                    // update countdown with server-authoritative week boundary
                    if (summary.weekStartMs > 0L) {
                        val nextFriday = summary.weekStartMs + 7L * 24 * 3_600_000L
                        _uiState.update { it.copy(nextFriday9AmMs = nextFriday, isLoading = false) }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG,"observeCommissionsData error: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun observeBalanceData() {
        viewModelScope.launch {
            observeBalance().collect { balance ->
                _uiState.update { it.copy(balance = balance.balance, balanceLoaded = true) }
            }
        }
    }

    private fun observeUnreadCount() {
        viewModelScope.launch {
            observeNotifications().collect { notifications ->
                _uiState.update { it.copy(unreadNotifications = notifications.count { !it.isRead }) }
            }
        }
    }

    private fun syncNotificationsNow() {
        val userId = sessionReader.currentSession().userId ?: return
        if (userId.isBlank()) return
        viewModelScope.launch {
            when (val result = notificationRepository.syncNotifications(userId)) {
                is com.autodrive.app.core.common.result.Result.Error ->
                    AppLogger.w(TAG, "notifications sync failed: ${result.message}")
                else -> Unit
            }
        }
    }

    private suspend fun refreshWeeklyTarget() {
        runCatching { weeklyPerformanceApi.getSnapshot() }
            .onSuccess { snapshot ->
                val serverTarget = Money.of(snapshot.weeklyTarget)
                dashboardPreferences.weeklyTarget = serverTarget
                _uiState.update { it.copy(weeklyTarget = serverTarget) }
            }
            .onFailure { error ->
                // Local preference is a deliberate offline cache. The server remains canonical.
                AppLogger.w(TAG, "weekly target refresh failed: ${error.message}")
            }
    }

    fun loadUnreadCount() { syncNotificationsNow() }

    fun loadData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            syncCoordinator.requestSync(SyncReason.USER_REFRESH)
            refreshWeeklyTarget()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            syncCoordinator.requestSync(SyncReason.USER_REFRESH)
            refreshWeeklyTarget()
            syncNotificationsNow()
            _uiState.update { it.copy(isRefreshing = false) }
            syncDynamoFromSupabase()
            pickAndShowMessage()
        }
        loadInsight(forceReload = true)
    }

    // ─── AI Insight ───────────────────────────────

    private fun loadInsight(forceReload: Boolean = false) {
        val userId = sessionReader.currentSession().userId ?: return
        if (!forceReload && _uiState.value.insights.isNotEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isInsightLoading = true, insightError = false) }
            val list = aiInsightRepository.getLatestN(userId)
            _uiState.update {
                it.copy(
                    insights            = list,
                    currentInsightIndex = 0,
                    isInsightLoading    = false,
                    insightError        = list.isEmpty(),
                )
            }
            startRotation(list.size)
        }
    }

    private fun startRotation(count: Int) {
        rotationJob?.cancel()
        if (count <= 1) return
        rotationJob = viewModelScope.launch {
            while (isActive) {
                delay(8_000)
                _uiState.update { it.copy(currentInsightIndex = (it.currentInsightIndex + 1) % count) }
            }
        }
    }

    // ─── Pump ─────────────────────────────────────

    private fun applySummary(summary: CommissionSummary) {
        val newTotal       = summary.weeklyTotal
        val currentDisplay = _uiState.value.displayedTotal
        val weekChanged    = summary.weekStartMs > 0L &&
            summary.weekStartMs != dashboardPreferences.lastDisplayedWeekStartMs

        when {
            weekChanged -> {
                dashboardPreferences.lastDisplayedWeekStartMs = summary.weekStartMs
                dashboardPreferences.lastDisplayedTotal = newTotal
                _uiState.update {
                    it.copy(summary = summary, syncedTotal = newTotal, displayedTotal = newTotal)
                }
            }
            newTotal < currentDisplay -> {
                dashboardPreferences.lastDisplayedTotal = newTotal
                _uiState.update { it.copy(summary = summary, syncedTotal = newTotal, displayedTotal = newTotal) }
            }
            currentDisplay.isZero() && newTotal.isPositive() -> {
                dashboardPreferences.lastDisplayedTotal = newTotal
                _uiState.update { it.copy(summary = summary, syncedTotal = newTotal, displayedTotal = newTotal) }
            }
            else -> _uiState.update { it.copy(summary = summary, syncedTotal = newTotal) }
        }
    }

    fun onPumpTapped() {
        if (_uiState.value.isPumping) return
        val state = _uiState.value
        if (state.syncedTotal <= state.displayedTotal) {
            BenzineSound.playEmptyTick()
            return
        }
        _uiState.update { it.copy(isPumping = true) }
    }

    fun onPumpAnimationComplete() {
        val newTotal = _uiState.value.syncedTotal
        dashboardPreferences.lastDisplayedTotal = newTotal
        _uiState.update { it.copy(displayedTotal = newTotal, isPumping = false) }
    }
}

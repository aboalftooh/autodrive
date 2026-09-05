package com.autodrive.app.feature.reports.presentation.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.core.common.format.FormatUtils
import com.autodrive.app.core.model.money.Money
import com.autodrive.app.feature.balance.domain.model.MarketerBalance
import com.autodrive.app.feature.balance.domain.usecase.ObserveBalanceUseCase
import com.autodrive.app.feature.commission.domain.CommissionCalculator
import com.autodrive.app.feature.commission.domain.model.CommissionEntry
import com.autodrive.app.feature.commission.domain.model.CommissionSummary
import com.autodrive.app.feature.commission.domain.model.Invoice
import com.autodrive.app.feature.commission.domain.usecase.ObserveCommissionsUseCase
import com.autodrive.app.feature.commission.domain.usecase.ObserveInvoicesUseCase
import com.autodrive.app.feature.competition.domain.usecase.ObserveWeeklyCompetitionUseCase
import com.autodrive.app.feature.profile.domain.usecase.ObserveProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val WEEK_MS = 7L * 24L * 60L * 60L * 1_000L

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val observeCommissions: ObserveCommissionsUseCase,
    private val observeInvoices: ObserveInvoicesUseCase,
    private val observeBalance: ObserveBalanceUseCase,
    private val observeProfile: ObserveProfileUseCase,
    private val observeWeeklyCompetition: ObserveWeeklyCompetitionUseCase,
    private val calculator: CommissionCalculator
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsUiState())
    val state: StateFlow<ReportsUiState> = _state.asStateFlow()

    private var reportsJob: Job? = null
    private var competitionJob: Job? = null

    init {
        viewModelScope.launch {
            observeProfile()
                .catch { /* Profile metadata must not replace report content with fake values. */ }
                .collect { user ->
                    val date = FormatUtils.formatJoinDateShort(user?.createdAt ?: "")
                    _state.update { it.copy(joinDate = date) }
                }
        }

        startReportsCollection()
    }

    /**
     * The screen already owns CompetitionAvailability. It explicitly opens/closes this stream so
     * Reports never asks competition RPCs while the server gate is DISABLED or LOCKED.
     */
    fun setCompetitionActive(isActive: Boolean) {
        if (!isActive) {
            competitionJob?.cancel()
            competitionJob = null
            _state.update { it.copy(winCount = null) }
            return
        }
        if (competitionJob?.isActive == true) return

        competitionJob = viewModelScope.launch {
            runCatching { observeWeeklyCompetition.refresh() }
            observeWeeklyCompetition().collect { data ->
                _state.update { it.copy(winCount = data.myWinCount) }
            }
        }
    }

    fun retryReports() {
        _state.update { current ->
            if (current.loadState == ReportsLoadState.CONTENT) {
                current.copy(errorMessage = null)
            } else {
                current.copy(loadState = ReportsLoadState.LOADING, errorMessage = null)
            }
        }
        startReportsCollection()
    }

    private fun startReportsCollection() {
        reportsJob?.cancel()
        reportsJob = viewModelScope.launch {
            combine(
                observeCommissions(),
                observeInvoices(),
                observeBalance()
            ) { (summary, entries), invoices, balance ->
                buildReportsContent(
                    summary = summary,
                    allEntries = entries,
                    allInvoices = invoices,
                    balance = balance,
                    calculator = calculator,
                    joinDate = _state.value.joinDate,
                    winCount = _state.value.winCount
                )
            }.catch { throwable ->
                _state.update { current ->
                    current.withReportsFailure(
                        throwable.message?.takeIf { it.isNotBlank() } ?: "تعذر تحميل التقارير"
                    )
                }
            }.collect { content ->
                _state.value = content.copy(
                    joinDate = _state.value.joinDate,
                    winCount = _state.value.winCount
                )
            }
        }
    }
}

internal fun buildReportsContent(
    summary: CommissionSummary,
    allEntries: List<CommissionEntry>,
    allInvoices: List<Invoice>,
    balance: MarketerBalance,
    calculator: CommissionCalculator,
    joinDate: String = "",
    winCount: Int? = null
): ReportsUiState {
    val currentWeekStart = summary.weekStartMs
    require(currentWeekStart > 0L) { "CommissionSummary.weekStartMs must be server-authoritative" }

    val currentWeekEnd = currentWeekStart + WEEK_MS
    val previousStart = currentWeekStart - WEEK_MS
    val previousEnd = currentWeekStart

    val currentWeekInvoices = allInvoices.filter { invoice ->
        calculator.parseIsoMs(invoice.createdAt) in currentWeekStart until currentWeekEnd
    }
    val previousWeekInvoices = allInvoices.filter { invoice ->
        calculator.parseIsoMs(invoice.createdAt) in previousStart until previousEnd
    }
    val currentWeekEntries = allEntries.filter { entry ->
        calculator.parseIsoMs(entry.createdAt) in currentWeekStart until currentWeekEnd
    }
    val previousWeekEntries = allEntries.filter { entry ->
        calculator.parseIsoMs(entry.createdAt) in previousStart until previousEnd
    }

    val currentWeekPurchases = Money.sum(currentWeekInvoices.map { it.totalAmount })
    val previousWeekPurchases = Money.sum(previousWeekInvoices.map { it.totalAmount })
    val currentWeekCommissions = Money.sum(currentWeekEntries.map { it.amount })
    val previousWeekCommissions = Money.sum(previousWeekEntries.map { it.amount })

    val dateFmt = SimpleDateFormat("d/M", Locale("ar"))
    val currentWeekLabel = "${dateFmt.format(Date(currentWeekStart))} — ${dateFmt.format(Date(currentWeekEnd))}"

    return ReportsUiState(
        loadState = ReportsLoadState.CONTENT,
        errorMessage = null,
        joinDate = joinDate,
        currentWeekLabel = currentWeekLabel,
        currentWeekPurchases = currentWeekPurchases,
        previousWeekPurchases = previousWeekPurchases,
        purchaseTrend = compareTrend(currentWeekPurchases, previousWeekPurchases),
        currentWeekCommissions = currentWeekCommissions,
        previousWeekCommissions = previousWeekCommissions,
        commissionTrend = compareTrend(currentWeekCommissions, previousWeekCommissions),
        currentWeekInvoiceCount = currentWeekInvoices.size,
        previousWeekInvoiceCount = previousWeekInvoices.size,
        balance = balance.balance,
        pending = summary.pending,
        lifetimeCommissions = Money.sum(allEntries.map { it.amount }),
        winCount = winCount
    )
}

internal fun compareTrend(current: Money, previous: Money): TrendComparison {
    require(current.currencyCode == previous.currencyCode) { "Trend currency mismatch" }

    if (previous.isZero()) {
        return if (current.isPositive()) {
            TrendComparison(TrendDirection.NEW, null)
        } else {
            TrendComparison(TrendDirection.FLAT, 0)
        }
    }

    val comparison = current.compareTo(previous)
    if (comparison == 0) return TrendComparison(TrendDirection.FLAT, 0)

    val percent = current.amount
        .subtract(previous.amount)
        .abs()
        .multiply(BigDecimal.valueOf(100L))
        .divide(previous.amount.abs(), 0, RoundingMode.HALF_UP)
        .intValueExact()

    return TrendComparison(
        direction = if (comparison > 0) TrendDirection.UP else TrendDirection.DOWN,
        percent = percent
    )
}

internal fun ReportsUiState.withReportsFailure(message: String): ReportsUiState =
    if (loadState == ReportsLoadState.CONTENT) {
        copy(errorMessage = message)
    } else {
        copy(loadState = ReportsLoadState.ERROR, errorMessage = message)
    }

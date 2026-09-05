package com.autodrive.app.feature.reports.presentation.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveFinance
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveSurface
import com.autodrive.app.core.designsystem.foundation.color.AutoDriveText
import com.autodrive.app.core.designsystem.patterns.header.ScreenHeader
import com.autodrive.app.core.designsystem.patterns.state.ErrorScreen
import com.autodrive.app.feature.competition.domain.model.WinWeek
import com.autodrive.app.feature.competition.domain.repository.WeeklyCompetitionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class WinWeeksViewModel @Inject constructor(
    private val repository: WeeklyCompetitionRepository
) : ViewModel() {

    private val _winWeeks = MutableStateFlow<List<WinWeek>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val winWeeks = _winWeeks.asStateFlow()
    val isLoading = _isLoading.asStateFlow()
    val errorMessage = _errorMessage.asStateFlow()

    init {
        loadWinWeeks()
    }

    fun retry() = loadWinWeeks()

    private fun loadWinWeeks() {
        if (_isLoading.value) return
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            runCatching { repository.getWinWeeks() }
                .onSuccess { _winWeeks.value = it }
                .onFailure { _errorMessage.value = "تعذر تحميل أسابيع الفوز" }
            _isLoading.value = false
        }
    }
}

@Composable
fun WinWeeksScreen(
    onBack: () -> Unit,
    viewModel: WinWeeksViewModel = hiltViewModel()
) {
    val winWeeks by viewModel.winWeeks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        containerColor = AutoDriveSurface.Canvas,
        topBar = {
            ScreenHeader(
                title = "أسابيع الفوز",
                onBack = onBack,
            )
        }
    ) { padding ->
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            when {
                isLoading && winWeeks.isEmpty() -> {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = AutoDriveFinance.Pending)
                    }
                }

                errorMessage != null && winWeeks.isEmpty() -> ErrorScreen(
                    title = "تعذر تحميل أسابيع الفوز",
                    body = "تحقق من الاتصال وحاول مجدداً",
                    retryLabel = "إعادة المحاولة",
                    onRetry = viewModel::retry,
                    modifier = Modifier.fillMaxSize().padding(padding),
                )

                winWeeks.isEmpty() -> {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("👑", fontSize = 48.sp)
                            Text(
                                text = "لم تحصل على شارة زعيم الأسبوع بعد",
                                color = AutoDriveText.Disabled,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "إجمالي مرات الفوز: ${winWeeks.size}",
                            style = MaterialTheme.typography.titleMedium,
                            color = AutoDriveFinance.Pending,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    items(winWeeks) { week ->
                        WinWeekRow(week)
                    }
                }
            }
        }
    }
}

@Composable
private fun WinWeekRow(week: WinWeek) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AutoDriveSurface.Raised, RoundedCornerShape(14.dp))
            .border(1.dp, AutoDriveFinance.Pending.copy(alpha = 0.40f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("👑", fontSize = 20.sp)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = "الأسبوع من ${week.weekStartLabel} إلى ${week.weekEndLabel}",
                style = MaterialTheme.typography.labelLarge,
                color = AutoDriveFinance.Pending,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "حصلت على شارة زعيم الأسبوع",
                style = MaterialTheme.typography.labelSmall,
                color = AutoDriveText.Secondary
            )
        }
    }
}

package com.autodrive.app.feature.reports.presentation.log

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodrive.app.feature.commission.domain.model.CommissionStatus
import com.autodrive.app.feature.commission.domain.model.Invoice
import com.autodrive.app.feature.commission.domain.model.InvoiceItem
import com.autodrive.app.feature.commission.domain.model.InvoiceStatus
import com.autodrive.app.feature.reports.domain.usecase.GetInvoiceDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvoiceDetailViewModel @Inject constructor(
    private val getInvoiceDetails: GetInvoiceDetailsUseCase,
) : ViewModel() {

    private val _invoice = MutableStateFlow<Invoice?>(null)
    val invoice: StateFlow<Invoice?> = _invoice.asStateFlow()

    private val _items = MutableStateFlow<List<InvoiceItem>>(emptyList())
    val items: StateFlow<List<InvoiceItem>> = _items.asStateFlow()

    var isLoading by mutableStateOf(true)
        private set

    fun load(invoiceId: String) {
        viewModelScope.launch {
            isLoading = true
            runCatching { getInvoiceDetails(invoiceId) }
                .onSuccess { details ->
                    _invoice.value = details.invoice
                    _items.value = details.items
                }
                .onFailure {
                    _invoice.value = null
                    _items.value = emptyList()
                }
            isLoading = false
        }
    }

    fun commissionStatus(invoice: Invoice): CommissionStatus =
        when (invoice.status) {
            InvoiceStatus.CLOSED_CASH, InvoiceStatus.CLOSED_CREDIT -> CommissionStatus.WITHDRAWABLE
            InvoiceStatus.OPEN                                      -> CommissionStatus.PENDING
            InvoiceStatus.CANCELLED                                 -> CommissionStatus.PAID
        }
}

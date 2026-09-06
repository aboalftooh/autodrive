package com.autodrive.app.core.sync.data

import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.core.network.dto.InvoiceDto
import com.autodrive.app.core.network.dto.PaymentDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reconciles billing realtime signals through authoritative, tenant-scoped PostgREST reads.
 * Realtime payloads identify what changed; they are not applied as financial state.
 */
@Singleton
class BillingTargetedRefresher @Inject constructor(
    private val supabase: AutoDriveSupabase,
    private val db: AutoDriveDatabase,
) {
    suspend fun refreshInvoice(invoiceId: String, clientId: String) {
        val invoice = supabase.client.postgrest["invoices"]
            .select(Columns.ALL) {
                filter {
                    eq("id", invoiceId)
                    eq("client_id", clientId)
                }
                limit(1)
            }
            .decodeSingleOrNull<InvoiceDto>()

        if (invoice == null || invoice.category != "SALE" || invoice.commission <= BigDecimal.ZERO) {
            db.paymentDao().deleteByInvoiceIds(listOf(invoiceId))
            db.invoiceDao().deleteById(invoiceId)
            return
        }

        db.invoiceDao().upsert(invoice.toEntity())
        refreshPaymentsForInvoice(invoiceId, clientId)
    }

    suspend fun refreshPayment(paymentId: String, invoiceId: String, clientId: String) {
        // Pull the invoice first when possible, but payment ownership never depends on Room ordering.
        val invoice = supabase.client.postgrest["invoices"]
            .select(Columns.ALL) {
                filter {
                    eq("id", invoiceId)
                    eq("client_id", clientId)
                }
                limit(1)
            }
            .decodeSingleOrNull<InvoiceDto>()

        if (invoice != null && invoice.category == "SALE" && invoice.commission > BigDecimal.ZERO) {
            db.invoiceDao().upsert(invoice.toEntity())
        }

        val payment = supabase.client.postgrest["payments"]
            .select(Columns.ALL) {
                filter {
                    eq("id", paymentId)
                    eq("client_id", clientId)
                }
                limit(1)
            }
            .decodeSingleOrNull<PaymentDto>()

        if (payment == null) {
            db.paymentDao().deleteById(paymentId)
            return
        }

        // If the invoice is temporarily unavailable, keep the tenant-owned payment. A later invoice
        // signal/full sync reconciles eligibility without losing this event because of arrival order.
        if (invoice == null || (invoice.category == "SALE" && invoice.commission > BigDecimal.ZERO)) {
            db.paymentDao().upsert(payment.toEntity())
        } else {
            db.paymentDao().deleteById(paymentId)
        }
    }

    private suspend fun refreshPaymentsForInvoice(invoiceId: String, clientId: String) {
        val payments = supabase.client.postgrest["payments"]
            .select(Columns.ALL) {
                filter {
                    eq("invoice_id", invoiceId)
                    eq("client_id", clientId)
                }
            }
            .decodeList<PaymentDto>()
        db.paymentDao().upsertAll(payments.distinctBy { it.id }.map { it.toEntity() })
    }
}

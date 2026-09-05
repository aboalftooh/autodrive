package com.autodrive.app.feature.balance.domain

import com.autodrive.app.core.model.money.Money
import com.autodrive.app.feature.balance.domain.model.WithdrawalRequest
import com.autodrive.app.feature.balance.domain.model.WithdrawalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WithdrawalPolicyTest {
    private val policy = WithdrawalPolicy()

    @Test
    fun approved_isStillProcessing_untilCompleted() {
        assertTrue(policy.isProcessing(WithdrawalStatus.APPROVED))
        assertTrue(policy.isProcessing(WithdrawalStatus.PENDING))
    }

    @Test
    fun activeRequest_blocksSecondWithdrawal_withoutSubtractingItsAmountFromBalance() {
        val decision = policy.evaluate(
            amount = Money.of("10"),
            balance = Money.of("100"),
            requests = listOf(request("active", "90", WithdrawalStatus.APPROVED)),
        )

        assertSame(WithdrawalDecision.ActiveRequestExists, decision)
    }

    @Test
    fun completedRequest_doesNotBlockNextWithdrawal() {
        val decision = policy.evaluate(
            amount = Money.of("80"),
            balance = Money.of("100"),
            requests = listOf(request("done", "90", WithdrawalStatus.COMPLETED)),
        )

        assertSame(WithdrawalDecision.Allowed, decision)
        assertNull(policy.activeRequest(listOf(request("done", "90", WithdrawalStatus.COMPLETED))))
    }

    @Test
    fun requestAboveCurrentBalance_isRejected() {
        val decision = policy.evaluate(
            amount = Money.of("101"),
            balance = Money.of("100"),
            requests = emptyList(),
        )

        assertEquals(WithdrawalDecision.InsufficientBalance(Money.of("100")), decision)
    }

    @Test
    fun zeroAmount_isRejected() {
        val decision = policy.evaluate(
            amount = Money.ZERO,
            balance = Money.of("100"),
            requests = emptyList(),
        )

        assertSame(WithdrawalDecision.InvalidAmount, decision)
    }

    private fun request(id: String, amount: String, status: WithdrawalStatus) = WithdrawalRequest(
        id = id,
        amount = Money.of(amount),
        status = status,
        bankName = "bank",
        bankAccount = "account",
        note = null,
        createdAt = "2026-09-04T10:00:00Z",
        completedAt = if (status == WithdrawalStatus.COMPLETED) "2026-09-04T11:00:00Z" else null,
    )
}

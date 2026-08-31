package com.autodrive.app.feature.auth.presentation.register

import com.autodrive.app.core.session.domain.CurrentSession
import com.autodrive.app.core.session.domain.SessionReader
import org.junit.Assert.assertEquals
import org.junit.Test

class RegisterViewModelTest {

    @Test
    fun `saveDraft always replaces tampered draft phone with verified session phone`() {
        val draft = RegistrationDraftStore().apply {
            phone = "249999999999"
            fullName = " Test User "
            bankName = " Bank "
            bankAccount = " 123 "
        }
        val viewModel = RegisterViewModel(
            sessionReader = FixedSessionReader(CurrentSession(phone = "249912345678")),
            draftStore = draft,
        )

        viewModel.saveDraft()

        assertEquals("249912345678", draft.phone)
        assertEquals("Test User", draft.fullName)
        assertEquals("Bank", draft.bankName)
        assertEquals("123", draft.bankAccount)
    }

    @Test
    fun `verifiedPhone is sourced only from current session`() {
        val draft = RegistrationDraftStore().apply { phone = "249999999999" }
        val viewModel = RegisterViewModel(
            sessionReader = FixedSessionReader(CurrentSession(phone = "249911111111")),
            draftStore = draft,
        )

        assertEquals("249911111111", viewModel.verifiedPhone)
    }

    private class FixedSessionReader(
        private val session: CurrentSession,
    ) : SessionReader {
        override fun currentSession(): CurrentSession = session
    }
}

package com.autodrive.app.feature.auth.presentation.register

import com.autodrive.app.core.common.registration.RegistrationProfileWriter
import com.autodrive.app.core.common.result.Result
import com.autodrive.app.core.model.account.AccountType
import com.autodrive.app.core.model.account.AutoDriveUser
import com.autodrive.app.core.session.domain.CurrentSession
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.feature.auth.domain.model.JoinRequestStatus
import com.autodrive.app.feature.auth.domain.model.PhoneEntryResult
import com.autodrive.app.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `saveDraft always replaces tampered draft phone with entry session phone`() {
        val draft = RegistrationDraftStore().apply {
            phone = "249999999999"
            fullName = " Test User "
            bankName = " Bank "
            bankAccount = " 123 "
        }
        val viewModel = newViewModel(
            session = CurrentSession(phone = "249912345678"),
            draft = draft,
        )

        viewModel.saveDraft()

        assertEquals("249912345678", draft.phone)
        assertEquals("Test User", draft.fullName)
        assertEquals("Bank", draft.bankName)
        assertEquals("123", draft.bankAccount)
    }

    @Test
    fun `pre OTP registration submits approval request only`() = runTest(dispatcher) {
        val auth = FakeAuthRepository().apply { submitResult = Result.Success("request-1") }
        val writer = FakeRegistrationWriter()
        val viewModel = newViewModel(
            session = CurrentSession(isLoggedIn = false, phone = "249912345678"),
            auth = auth,
            writer = writer,
        ).apply {
            accountType = "MARKETER"
            fullName = "Ahmed Ali"
        }

        viewModel.submitOrComplete()
        advanceUntilIdle()

        assertEquals(listOf(Triple("249912345678", "Ahmed Ali", "MARKETER")), auth.submissions)
        assertTrue(viewModel.action.value is RegistrationActionState.Submitted)
        assertTrue(writer.savedUsers.isEmpty())
    }

    @Test
    fun `post OTP onboarding writes only authenticated membership profile`() = runTest(dispatcher) {
        val auth = FakeAuthRepository()
        val writer = FakeRegistrationWriter().apply { result = Result.Success(Unit) }
        val session = CurrentSession(
            isLoggedIn = true,
            userId = "user-1",
            clientId = "client-1",
            orgId = "org-1",
            accountType = "WORKSHOP_OWNER",
            phone = "249912345678",
        )
        val viewModel = newViewModel(session, auth = auth, writer = writer).apply {
            accountType = "MARKETER"
            fullName = "Workshop Owner"
            bankName = "Bank"
            bankAccount = "123"
            workshopName = "Garage"
            specialty = "ميكانيكا عامة"
            workersCount = "4"
            address = "Khartoum"
        }

        viewModel.submitOrComplete()
        advanceUntilIdle()

        assertTrue(auth.submissions.isEmpty())
        assertEquals(1, writer.savedUsers.size)
        val saved = writer.savedUsers.single()
        assertEquals("user-1", saved.userId)
        assertEquals("client-1", saved.clientId)
        assertEquals("org-1", saved.orgId)
        assertEquals("249912345678", saved.phone)
        assertEquals(AccountType.WORKSHOP_OWNER, saved.accountType)
        assertTrue(viewModel.action.value is RegistrationActionState.Completed)
    }

    private fun newViewModel(
        session: CurrentSession,
        draft: RegistrationDraftStore = RegistrationDraftStore(),
        auth: FakeAuthRepository = FakeAuthRepository(),
        writer: FakeRegistrationWriter = FakeRegistrationWriter(),
    ) = RegisterViewModel(
        sessionReader = FixedSessionReader(session),
        draftStore = draft,
        authRepository = auth,
        registrationProfileWriter = writer,
    )

    private class FixedSessionReader(private val session: CurrentSession) : SessionReader {
        override fun currentSession(): CurrentSession = session
    }

    private class FakeRegistrationWriter : RegistrationProfileWriter {
        var result: Result<Unit> = Result.Success(Unit)
        val savedUsers = mutableListOf<AutoDriveUser>()
        override suspend fun saveRegisteredUser(user: AutoDriveUser): Result<Unit> {
            savedUsers += user
            return result
        }
    }

    private class FakeAuthRepository : AuthRepository {
        var submitResult: Result<String> = Result.Success("request")
        val submissions = mutableListOf<Triple<String, String, String>>()

        override suspend fun enterPhone(phone: String): PhoneEntryResult = PhoneEntryResult.NewRequest
        override suspend fun sendPhoneOtp(phone: String): Result<String?> = Result.Success(null)
        override suspend fun verifyPhoneOtp(phone: String, otp: String): Result<Unit> = Result.Success(Unit)

        override suspend fun submitJoinRequest(phone: String, fullName: String, accountType: String): Result<String> {
            submissions += Triple(phone, fullName, accountType)
            return submitResult
        }

        override suspend fun getJoinRequestStatus(requestId: String): Result<JoinRequestStatus> = Result.Error("unused")
        override suspend fun sendApprovedPhoneOtp(phone: String, requestId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun verifyApprovedPhoneOtp(phone: String, otp: String, requestId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun restoreSession(): Boolean = false
        override suspend fun signOut() = Unit
        override fun isLoggedIn(): Boolean = false
        override fun isRegistrationComplete(): Boolean = false
        override fun getCurrentUserId(): String = ""
    }
}

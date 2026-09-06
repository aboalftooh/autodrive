package com.autodrive.app.session

import com.autodrive.app.core.model.money.Money

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.autodrive.app.core.session.domain.RegistrationState
import com.autodrive.app.core.session.data.PreferencesManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionSwitchIsolationTest {

    private lateinit var preferences: PreferencesManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        preferences = PreferencesManager(context)
        preferences.clearSession()
    }

    @After
    fun tearDown() {
        preferences.clearSession()
    }

    @Test
    fun switchingAccountsDoesNotRetainPreviousIdentityOrDashboardState() {
        preferences.updateSession { current ->
            current.copy(
                isLoggedIn = true,
                registrationState = RegistrationState.COMPLETE,
                userId = "user-a",
                clientId = "client-a",
                orgId = "org-a",
                userName = "Account A",
                accountType = "MARKETER",
                phone = "111",
                pendingInviteCode = "invite-a",
            )
        }
        preferences.weeklyTarget = Money.of(900_000L)
        preferences.lastDisplayedTotal = Money.of(50_000L)
        preferences.lastDisplayedWeekStartMs = 1234L

        preferences.clearSession()
        preferences.updateSession { current ->
            current.copy(isLoggedIn = true, userId = "user-b", clientId = "client-b")
        }

        val current = preferences.currentSession()
        assertEquals("user-b", current.userId)
        assertEquals("client-b", current.clientId)
        assertNull(current.orgId)
        assertNull(current.userName)
        assertNull(current.accountType)
        assertNull(current.phone)
        assertNull(current.pendingInviteCode)
        assertFalse(current.isRegistrationComplete)
        assertEquals(Money.of(500_000L), preferences.weeklyTarget)
        assertEquals(Money.ZERO, preferences.lastDisplayedTotal)
        assertEquals(0L, preferences.lastDisplayedWeekStartMs)
    }
}

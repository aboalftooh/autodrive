package com.autodrive.app.core.session.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrentSessionTest {

    @Test
    fun `registration completeness derives from state`() {
        assertFalse(CurrentSession().isRegistrationComplete)
        assertTrue(
            CurrentSession(registrationState = RegistrationState.COMPLETE)
                .isRegistrationComplete,
        )
    }
}

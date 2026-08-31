package com.autodrive.app.core.session.domain

enum class RegistrationState {
    INCOMPLETE,
    COMPLETE
}

data class CurrentSession(
    val isLoggedIn: Boolean = false,
    val registrationState: RegistrationState = RegistrationState.INCOMPLETE,
    val userId: String? = null,
    val clientId: String? = null,
    val orgId: String? = null,
    val userName: String? = null,
    val accountType: String? = null,
    val phone: String? = null,
    val pendingJoinRequestId: String? = null,
) {
    val isRegistrationComplete: Boolean
        get() = registrationState == RegistrationState.COMPLETE
}

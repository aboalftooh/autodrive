package com.autodrive.app.core.session.domain

/**
 * Mutation boundary for persisted session state.
 *
 * [updateSession] applies a single transformation to current session state; [clearSession] removes
 * account/session ownership. Callers coordinating logout/synchronization must preserve their higher-
 * level quiesce/clear ordering and must not use this interface to manufacture tenant identity.
 */
interface SessionWriter {
    fun updateSession(transform: (CurrentSession) -> CurrentSession)
    fun clearSession()
}

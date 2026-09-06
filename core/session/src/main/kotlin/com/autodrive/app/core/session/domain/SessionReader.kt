package com.autodrive.app.core.session.domain

/**
 * Read boundary for the process-local persisted account/session identity.
 *
 * Cross-module consumers use this contract instead of owning session persistence. Returned identity
 * is the basis for exact synchronization scope; consumers must treat missing/partial identity as an
 * unavailable scope rather than fall back to another account or tenant.
 */
interface SessionReader {
    fun currentSession(): CurrentSession
}

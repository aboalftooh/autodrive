package com.autodrive.app.core.sync.data

import com.autodrive.app.core.session.domain.CurrentSession

/**
 * Exact local synchronization/tenant scope: authenticated user, client, and organization.
 *
 * A scope is constructible from [CurrentSession] only when all three identifiers are non-blank.
 * Sensitive synchronization commits re-check the current session against this value so work from a
 * departed account/tenant cannot be committed under a new session.
 */
data class SyncScope(
    val userId: String,
    val clientId: String,
    val orgId: String,
) {
    companion object {
        fun from(session: CurrentSession): SyncScope? {
            val userId = session.userId?.takeIf { it.isNotBlank() } ?: return null
            val clientId = session.clientId?.takeIf { it.isNotBlank() } ?: return null
            val orgId = session.orgId?.takeIf { it.isNotBlank() } ?: return null
            return SyncScope(userId = userId, clientId = clientId, orgId = orgId)
        }
    }
}

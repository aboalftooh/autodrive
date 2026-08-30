package com.autodrive.app.core.sync.realtime

enum class RealtimeMutation {
    UPSERT,
    DELETE,
}

object RealtimeEventPolicy {
    fun acceptsClient(ownerClientId: String?, expectedClientId: String): Boolean =
        ownerClientId == expectedClientId

    fun acceptsUser(ownerUserId: String?, expectedUserId: String): Boolean =
        ownerUserId == expectedUserId

    fun mutation(isDelete: Boolean): RealtimeMutation =
        if (isDelete) RealtimeMutation.DELETE else RealtimeMutation.UPSERT
}

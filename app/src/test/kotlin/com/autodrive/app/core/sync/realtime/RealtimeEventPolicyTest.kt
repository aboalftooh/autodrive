package com.autodrive.app.core.sync.realtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeEventPolicyTest {

    @Test
    fun `client ownership rejects another account`() {
        assertTrue(RealtimeEventPolicy.acceptsClient("client-1", "client-1"))
        assertFalse(RealtimeEventPolicy.acceptsClient("client-2", "client-1"))
        assertFalse(RealtimeEventPolicy.acceptsClient(null, "client-1"))
    }

    @Test
    fun `user ownership rejects another user`() {
        assertTrue(RealtimeEventPolicy.acceptsUser("user-1", "user-1"))
        assertFalse(RealtimeEventPolicy.acceptsUser("user-2", "user-1"))
        assertFalse(RealtimeEventPolicy.acceptsUser(null, "user-1"))
    }

    @Test
    fun `delete action resolves to delete mutation`() {
        assertEquals(RealtimeMutation.DELETE, RealtimeEventPolicy.mutation(isDelete = true))
        assertEquals(RealtimeMutation.UPSERT, RealtimeEventPolicy.mutation(isDelete = false))
    }
}

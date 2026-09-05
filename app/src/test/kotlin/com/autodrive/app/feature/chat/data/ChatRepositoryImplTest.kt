package com.autodrive.app.feature.chat.data

import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.database.dao.ChatMessageDao
import com.autodrive.app.core.database.dao.ConversationDao
import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.core.common.result.Result
import com.autodrive.app.feature.chat.domain.model.MessageType
import com.autodrive.app.core.session.domain.CurrentSession
import com.autodrive.app.core.session.domain.SessionReader
import com.autodrive.app.core.sync.data.ChatRecoverySynchronizer
import com.autodrive.app.feature.chat.data.worker.ChatMediaTransferScheduler
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Phase 6.1 — اختبارات ChatRepositoryImpl
 *
 * يركّز على عقد الإصلاح في Phase 1.1: إذا كانت `CurrentSession.orgId` أو `CurrentSession.clientId`
 * أو `senderId` مفقودة، يجب:
 *   - إرجاع `Result.Error` فوراً
 *   - عدم تحديث حالة الرسالة المحلية إلى SENT
 *   - عدم لمس Supabase
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryImplTest {

    @MockK lateinit var supabase: AutoDriveSupabase
    @MockK lateinit var db: AutoDriveDatabase
    @MockK lateinit var sessionReader: SessionReader
    @MockK lateinit var chatDao: ChatMessageDao
    @MockK lateinit var convDao: ConversationDao
    @MockK lateinit var mediaManager: ChatMediaManager
    @MockK lateinit var chatRecovery: ChatRecoverySynchronizer
    @MockK lateinit var mediaTransferScheduler: ChatMediaTransferScheduler

    private lateinit var repo: ChatRepositoryImpl

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        every { db.chatMessageDao() } returns chatDao
        every { db.conversationDao() } returns convDao
        repo = ChatRepositoryImpl(supabase, db, sessionReader, mediaManager, chatRecovery, mediaTransferScheduler)
    }

    @Test
    fun `sendMessage - يفشل ولا يحدّث الحالة الى SENT عند orgId الفارغ`() = runTest {
        every { sessionReader.currentSession() } returns CurrentSession(clientId = "client-1")

        val result = repo.sendMessage(
            conversationId = "conv-1",
            senderId       = "user-1",
            type           = MessageType.TEXT,
            content        = "مرحبا"
        )

        assertTrue("يجب أن يكون النتيجة Error", result is Result.Error)
        val err = result as Result.Error
        assertTrue("الرسالة يجب أن تشير للجلسة", err.message.contains("جلسة"))

        // لم يُدخَل أي صف ولم تُحدَّث حالة الى SENT
        coVerify(exactly = 0) { chatDao.insert(any()) }
        coVerify(exactly = 0) { chatDao.updateStatus(any(), "SENT") }
    }

    @Test
    fun `sendMessage - يفشل عند clientId الفارغ`() = runTest {
        every { sessionReader.currentSession() } returns CurrentSession(orgId = "org-1")

        val result = repo.sendMessage("conv-1", "user-1", MessageType.TEXT, "مرحبا")

        assertTrue(result is Result.Error)
        coVerify(exactly = 0) { chatDao.updateStatus(any(), "SENT") }
        coVerify(exactly = 0) { chatDao.insert(any()) }
    }

    @Test
    fun `sendMessage - يفشل عند senderId فارغ (auth uid مفقود)`() = runTest {
        every { sessionReader.currentSession() } returns CurrentSession(orgId = "org-1", clientId = "client-1")

        val result = repo.sendMessage(
            conversationId = "conv-1",
            senderId       = "",
            type           = MessageType.TEXT,
            content        = "مرحبا"
        )

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("جلسة"))
        coVerify(exactly = 0) { chatDao.insert(any()) }
        coVerify(exactly = 0) { chatDao.updateStatus(any(), "SENT") }
    }

    @Test
    fun `retrySend - يفشل عند orgId الفارغ ولا يحدّث الحالة الى PENDING`() = runTest {
        every { sessionReader.currentSession() } returns CurrentSession(clientId = "client-1")

        val result = repo.retrySend("msg-1")

        assertTrue(result is Result.Error)
        coVerify(exactly = 0) { chatDao.updateStatus("msg-1", "PENDING") }
        coVerify(exactly = 0) { chatDao.updateStatus("msg-1", "SENT") }
    }

    @Test
    fun `retrySend - يفشل عند الرسالة غير موجودة`() = runTest {
        every { sessionReader.currentSession() } returns CurrentSession(orgId = "org-1", clientId = "client-1")
        io.mockk.coEvery { chatDao.getById("msg-x") } returns null

        val result = repo.retrySend("msg-x")

        assertTrue(result is Result.Error)
        assertEquals("الرسالة غير موجودة", (result as Result.Error).message)
    }
}

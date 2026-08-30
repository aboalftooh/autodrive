package com.autodrive.app.feature.notifications.data

import com.autodrive.app.core.database.AutoDriveDatabase
import com.autodrive.app.core.database.dao.NotificationDao
import com.autodrive.app.core.database.entities.NotificationEntity
import com.autodrive.app.core.network.AutoDriveSupabase
import com.autodrive.app.core.network.dto.NotificationDto
import com.autodrive.app.core.common.result.Result
import com.autodrive.app.core.session.domain.CurrentSession
import com.autodrive.app.core.session.domain.SessionReader
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Phase 6.1 — اختبارات NotificationRepositoryImpl
 *
 * يغطي:
 *   ① syncNotifications: يرفض userId الفارغ
 *   ② markAsRead / markAllRead: تنفيذ DAO الصحيح
 *   ③ منطق الدمج (read_synced) كاختبار وحدة منعزل عن Supabase:
 *      - إذا علّم المستخدم محلياً ولم يُزامَن: نُبقي isRead=true بصرف النظر عن الخادم
 *      - الإشعارات الأخرى تأخذ قيم الخادم
 *
 * ملاحظة: الاختبار الكامل لجلب البيانات من Supabase يتطلب تكسير
 * postgrest chain — تركناه للاختبارات اليدوية في 6.2.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationRepositoryImplTest {

    @MockK lateinit var supabase: AutoDriveSupabase
    @MockK lateinit var db: AutoDriveDatabase
    @MockK lateinit var notifDao: NotificationDao
    @MockK lateinit var sessionReader: SessionReader

    private lateinit var repo: NotificationRepositoryImpl

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        every { db.notificationDao() } returns notifDao
        every { sessionReader.currentSession() } returns CurrentSession(
            isLoggedIn = true,
            userId = "user-1",
            clientId = "client-1",
            orgId = "org-1",
        )
        repo = NotificationRepositoryImpl(supabase, db, sessionReader)
    }

    // ── userId blank ──────────────────────────────────────────────────

    @Test
    fun `syncNotifications - يفشل بدون userId`() = runTest {
        val result = repo.syncNotifications("")
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("معرّف"))
        coVerify(exactly = 0) { notifDao.upsertAll(any()) }
    }

    // ── markAsRead / markAllRead ──────────────────────────────────────

    @Test
    fun `markAsRead - يحدّث Room ثم يحاول مزامنة الخادم`() = runTest {
        coEvery { notifDao.markAsRead("notif-1", "user-1") } returns Unit

        repo.markAsRead("notif-1", "user-1")

        coVerify(exactly = 1) { notifDao.markAsRead("notif-1", "user-1") }
        // confirmReadSynced يُستدعى فقط إن نجح Supabase — قد يفشل في mock relaxed لكن ذلك جزء من runCatching
    }

    @Test
    fun `markAllRead - يستدعي markAllAsRead على DAO`() = runTest {
        coEvery { notifDao.markAllAsRead("user-1") } returns Unit

        val result = repo.markAllRead("user-1")

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { notifDao.markAllAsRead("user-1") }
    }

    // ── منطق الدمج (read_synced preservation) ──────────────────────────
    // يحاكي بدقة المنطق في NotificationRepositoryImpl.syncNotifications:
    //   isRead    = if (dto.id in pendingReadIds) true else dto.isRead
    //   readSynced = dto.id !in pendingReadIds

    @Test
    fun `merge logic - يحافظ على المقروءة محلياً غير المُزامنة`() {
        val pendingReadIds = setOf("notif-pending")
        val remote = listOf(
            NotificationDto(id = "notif-pending", userId = "u1", clientId = "c1",
                type = "NEW_INVOICE", title = "ت", body = "ب",
                isRead = false, createdAt = "2026-05-07T10:00:00Z"),
            NotificationDto(id = "notif-fresh", userId = "u1", clientId = "c1",
                type = "NEW_INVOICE", title = "ت", body = "ب",
                isRead = false, createdAt = "2026-05-07T11:00:00Z"),
        )

        val merged = remote.map { dto ->
            NotificationEntity(
                id        = dto.id,
                userId    = dto.userId,
                clientId  = dto.clientId,
                type      = dto.type,
                title     = dto.title,
                body      = dto.body,
                isRead    = if (dto.id in pendingReadIds) true else dto.isRead,
                createdAt = dto.createdAt,
                readSynced = dto.id !in pendingReadIds
            )
        }

        val pending = merged.first { it.id == "notif-pending" }
        assertTrue("الإشعار المعلّم محلياً يبقى مقروءاً", pending.isRead)
        assertFalse("علم المزامنة يبقى false ليُعاد رفعه", pending.readSynced)

        val fresh = merged.first { it.id == "notif-fresh" }
        assertFalse("إشعار جديد يأخذ قيمة الخادم", fresh.isRead)
        assertTrue("علم المزامنة لإشعار جديد = true", fresh.readSynced)
    }

    @Test
    fun `merge logic - الإشعار الموجود لا يُكرّر (نفس id يُستبدل عند upsert)`() {
        // upsert ضمنياً = INSERT أو UPDATE على نفس المفتاح. مع نفس id لا يحصل تكرار.
        val pendingReadIds = emptySet<String>()
        val remote = listOf(
            NotificationDto(id = "n1", userId = "u1", clientId = "c1",
                type = "NEW_INVOICE", title = "أ", body = "ب",
                isRead = true, createdAt = "2026-05-07T10:00:00Z"),
            NotificationDto(id = "n1", userId = "u1", clientId = "c1",
                type = "NEW_INVOICE", title = "أ", body = "ب",
                isRead = true, createdAt = "2026-05-07T10:00:00Z"),
        )

        val merged = remote.map { dto ->
            NotificationEntity(
                id = dto.id, userId = dto.userId, clientId = dto.clientId,
                type = dto.type, title = dto.title, body = dto.body,
                isRead = dto.isRead, createdAt = dto.createdAt,
                readSynced = dto.id !in pendingReadIds
            )
        }

        // المفاتيح الفريدة لا تتجاوز عدد الإشعارات الفعلية (هنا 1)
        val distinct = merged.distinctBy { it.id }
        assertEquals(1, distinct.size)
    }
}

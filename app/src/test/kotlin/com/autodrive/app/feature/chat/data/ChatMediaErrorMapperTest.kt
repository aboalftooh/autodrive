package com.autodrive.app.feature.chat.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatMediaErrorMapperTest {
    @Test fun `maps missing remote object`() = assertEquals(
        "تعذّر تجهيز الملف على الخادم. تأكد من اتصالك وحاول مجدداً.",
        ChatMediaErrorMapper.userMessage("Object not found"),
    )

    @Test fun `maps row level security denial`() = assertEquals(
        "ليست لديك صلاحية رفع الملفات. سجّل خروج وأعد إدخال كود الدعوة.",
        ChatMediaErrorMapper.userMessage("row-level security"),
    )

    @Test fun `maps oversized upload`() = assertEquals(
        "حجم الملف كبير جداً.",
        ChatMediaErrorMapper.userMessage("HTTP 413"),
    )

    @Test fun `maps network failure`() = assertEquals(
        "تعذّر الاتصال — تحقق من الإنترنت وأعد المحاولة.",
        ChatMediaErrorMapper.userMessage("network timeout"),
    )

    @Test fun `maps unknown failure safely`() = assertEquals(
        "تعذّر رفع الملف. حاول مرة أخرى.",
        ChatMediaErrorMapper.userMessage("unknown"),
    )
}

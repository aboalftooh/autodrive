package com.autodrive.app.feature.chat.data

object ChatMediaErrorMapper {
    fun userMessage(raw: String): String = when {
        raw.contains("not_found", true) || raw.contains("Object not found", true) ->
            "تعذّر تجهيز الملف على الخادم. تأكد من اتصالك وحاول مجدداً."
        raw.contains("row-level security", true) || raw.contains("violates row-level", true) ->
            "ليست لديك صلاحية رفع الملفات. سجّل خروج وأعد إدخال كود الدعوة."
        raw.contains("Payload too large", true) || raw.contains("413") ->
            "حجم الملف كبير جداً."
        raw.contains("network", true) || raw.contains("timeout", true) ->
            "تعذّر الاتصال — تحقق من الإنترنت وأعد المحاولة."
        else -> "تعذّر رفع الملف. حاول مرة أخرى."
    }
}

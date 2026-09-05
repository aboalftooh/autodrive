package com.autodrive.app.core.platform.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.autodrive.app.core.platform.BuildConfig

object WhatsAppHelper {

    private val adminPhone: String get() = BuildConfig.ADMIN_WHATSAPP

    /** FAB الداشبورد — تواصل مع الإدارة */
    fun openContactAdmin(context: Context, userName: String) {
        val message = "مرحباً، أنا $userName\n"
        openToNumber(context, adminPhone, message)
    }

    /** شاشة "ادخل الكود" — طلب كود انضمام جديد */
    fun requestInviteCode(context: Context, message: String? = null) {
        val body = message?.takeIf { it.isNotBlank() } ?: """
السلام عليكم
أرغب في الحصول على كود انضمام لتطبيق بنزين.
        """.trimIndent()
        openToNumber(context, adminPhone.ifBlank { "249123142758" }, body)
    }

    /** مشاركة الفاتورة — يفتح قائمة جهات الاتصال في واتساب */
    fun shareInvoice(context: Context, invoiceText: String) {
        val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_TEXT, invoiceText)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val businessIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("com.whatsapp.w4b")
            putExtra(Intent.EXTRA_TEXT, invoiceText)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val fallback = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, invoiceText)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(whatsappIntent)
        } catch (_: Exception) {
            try {
                context.startActivity(businessIntent)
            } catch (_: Exception) {
                context.startActivity(Intent.createChooser(fallback, "مشاركة الفاتورة").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
        }
    }

    private fun openToNumber(context: Context, phone: String, message: String) {
        val url = "https://wa.me/$phone?text=${Uri.encode(message)}"
        val whatsappIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage("com.whatsapp")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val businessIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage("com.whatsapp.w4b")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(whatsappIntent)
        } catch (_: Exception) {
            try {
                context.startActivity(businessIntent)
            } catch (_: Exception) {
                context.startActivity(browserIntent)
            }
        }
    }
}

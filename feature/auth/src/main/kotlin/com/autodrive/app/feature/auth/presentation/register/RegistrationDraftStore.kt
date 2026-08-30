package com.autodrive.app.feature.auth.presentation.register

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegistrationDraftStore @Inject constructor() {
    var accountType: String = "MARKETER"
    var fullName: String = ""
    var phone: String = ""
    var bankName: String = ""
    var bankAccount: String = ""
    var workshopName: String = ""
    var specialty: String = ""
    var workersCount: String = ""
    var address: String = ""

    fun clear() {
        accountType = "MARKETER"; fullName = ""; phone = ""; bankName = ""; bankAccount = ""
        workshopName = ""; specialty = ""; workersCount = ""; address = ""
    }

    fun whatsappMessage(): String = buildString {
        appendLine("السلام عليكم")
        appendLine("أرغب في الحصول على كود انضمام لتطبيق بنزين.")
        appendLine("الاسم: $fullName")
        appendLine("رقم الهاتف: $phone")
        appendLine("نوع الحساب: ${if (accountType == "WORKSHOP_OWNER") "صاحب ورشة" else "مسوق"}")
        if (accountType == "WORKSHOP_OWNER") {
            appendLine("اسم الورشة: $workshopName")
            appendLine("التخصص: $specialty")
            appendLine("العنوان: $address")
        }
        appendLine("البنك: $bankName")
        append("رقم الحساب: $bankAccount")
    }
}

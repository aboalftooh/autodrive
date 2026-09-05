package com.autodrive.app.core.model.account

enum class AccountType(val label: String) {
    MARKETER("مسوّق"),
    WORKSHOP_OWNER("صاحب ورشة")
}

data class AutoDriveUser(
    val id: String,
    val userId: String,
    val clientId: String,
    val orgId: String,
    val accountType: AccountType,
    val fullName: String,
    val phone: String,
    val bankName: String?,
    val bankAccount: String?,
    val workshopName: String? = null,
    val specialty: String? = null,
    val workersCount: Int? = null,
    val address: String? = null,
    val createdAt: String = ""
)

object WorkshopSpecialties {
    val labels: List<String> = listOf(
        "ميكانيكا عامة",
        "كهرباء سيارات",
        "سمكرة ودهان",
        "تكييف سيارات",
        "إطارات وبطاريات",
        "زيوت وصيانة",
    )
}

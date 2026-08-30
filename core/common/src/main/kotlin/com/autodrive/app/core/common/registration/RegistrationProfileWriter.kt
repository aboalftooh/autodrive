package com.autodrive.app.core.common.registration

import com.autodrive.app.core.common.result.Result
import com.autodrive.app.core.model.account.AutoDriveUser

/**
 * منفذ عابر للميزات لحفظ ملف المستخدم بعد اكتمال التسجيل.
 * يوجد العقد في Core لتجنب اعتماد Feature على Feature أو Bridge داخل :app.
 */
interface RegistrationProfileWriter {
    suspend fun saveRegisteredUser(user: AutoDriveUser): Result<Unit>
}

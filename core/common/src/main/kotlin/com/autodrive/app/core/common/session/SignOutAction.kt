package com.autodrive.app.core.common.session

/** منفذ تسجيل الخروج الذي تستهلكه الواجهات دون الاعتماد على Feature المصادقة. */
interface SignOutAction {
    suspend operator fun invoke()
}

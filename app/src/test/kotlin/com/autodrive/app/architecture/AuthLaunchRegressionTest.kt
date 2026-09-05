package com.autodrive.app.architecture

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AuthLaunchRegressionTest {
    private fun root(): File {
        var f=File(System.getProperty("user.dir"))
        repeat(6){ if(File(f,"settings.gradle.kts").exists()) return f; f=f.parentFile ?: return@repeat }
        return f
    }
    private fun text(path:String)=File(root(),path).readText()
    @Test fun `phone split is existing otp or new join code only`() {
        val vm=text("feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/presentation/login/PhoneAuthViewModel.kt")
        assertTrue(vm.contains("PhoneEntryResult.LoginOtp")); assertTrue(vm.contains("PhoneEntryResult.JoinCodeRequired"))
        assertFalse(vm.contains("WaitApproval")); assertFalse(vm.contains("ApprovedOtp")); assertFalse(vm.contains("NewRequest"))
    }
    @Test fun `new user route is join code then common otp`() {
        val nav=text("app/src/main/kotlin/com/autodrive/app/navigation/NavigationGraphs.kt")
        assertTrue(nav.contains("Screen.CodeInput")); assertTrue(nav.contains("Screen.OtpInput")); assertFalse(nav.contains("Screen.Waiting")); assertFalse(nav.contains("Screen.AccountType"))
    }
    @Test fun `repository has no approval otp endpoints`() {
        val repo=text("feature/auth/src/main/kotlin/com/autodrive/app/feature/auth/data/AuthRepositoryImpl.kt")
        assertTrue(repo.contains("verify_join_code")); assertTrue(repo.contains("function = \"send-phone-otp\"")); assertTrue(repo.contains("function = \"verify-phone-otp\""))
        assertFalse(repo.contains("autodrive-send-otp")); assertFalse(repo.contains("autodrive-verify-otp")); assertFalse(repo.contains("submitJoinRequest")); assertFalse(repo.contains("getJoinRequestStatus"))
    }
    @Test fun `session uses invite code not join request id`() {
        val session=text("core/session/src/main/kotlin/com/autodrive/app/core/session/domain/CurrentSession.kt")
        assertTrue(session.contains("pendingInviteCode")); assertFalse(session.contains("pendingJoinRequestId"))
    }
}

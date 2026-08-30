package com.autodrive.app.core.designsystem.foundation.typography

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.autodrive.app.core.designsystem.R

val AutoDriveTajawal = FontFamily(
    Font(R.font.tajawal_regular, FontWeight.Normal),
    Font(R.font.tajawal_medium, FontWeight.Medium),
    Font(R.font.tajawal_bold, FontWeight.Bold),
    Font(R.font.tajawal_extrabold, FontWeight.ExtraBold),
)

val AutoDriveTypography = Typography(
    displayLarge = TextStyle(fontFamily = AutoDriveTajawal, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp, lineHeight = 56.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = AutoDriveTajawal, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    displaySmall = TextStyle(fontFamily = AutoDriveTajawal, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(fontFamily = AutoDriveTajawal, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineMedium = TextStyle(fontFamily = AutoDriveTajawal, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    headlineSmall = TextStyle(fontFamily = AutoDriveTajawal, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = AutoDriveTajawal, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = AutoDriveTajawal, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontFamily = AutoDriveTajawal, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = AutoDriveTajawal, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = AutoDriveTajawal, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = AutoDriveTajawal, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = AutoDriveTajawal, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 0.5.sp),
    labelMedium = TextStyle(fontFamily = AutoDriveTajawal, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = AutoDriveTajawal, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

val AutoDriveStatXL = TextStyle(
    fontFamily = AutoDriveTajawal,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 60.sp,
    lineHeight = 68.sp,
)

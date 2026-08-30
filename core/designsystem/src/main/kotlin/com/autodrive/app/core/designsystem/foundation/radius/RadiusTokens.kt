package com.autodrive.app.core.designsystem.foundation.radius

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object AutoDriveRadius {
    val None = 0.dp
    val SM = 8.dp
    val MD = 12.dp
    val LG = 16.dp
    val XL = 20.dp
    val X2L = 24.dp
    val Full = 999.dp

    val SmallShape = RoundedCornerShape(SM)
    val MediumShape = RoundedCornerShape(MD)
    val LargeShape = RoundedCornerShape(LG)
    val ExtraLargeShape = RoundedCornerShape(XL)
    val HeroShape = RoundedCornerShape(X2L)
    val PillShape = RoundedCornerShape(Full)
}

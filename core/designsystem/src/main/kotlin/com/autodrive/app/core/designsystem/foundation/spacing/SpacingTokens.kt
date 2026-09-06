package com.autodrive.app.core.designsystem.foundation.spacing

import androidx.compose.ui.unit.dp

object AutoDriveSpace {
    val Optical = 2.dp
    val XS = 4.dp
    val SM = 8.dp
    val MD = 12.dp
    val LG = 16.dp
    val XL = 20.dp
    val X2L = 24.dp
    val X3L = 32.dp
    val X4L = 40.dp
    val X5L = 48.dp
    val X6L = 64.dp
}

/** Width limits validated during Session 09 Visual QA. */
object AutoDriveContentWidth {
    /** Single-column reading surfaces: Conversations and Settings. */
    val Readable = 600.dp

    /** Dashboard/financial surfaces preserve hierarchy up to the expanded breakpoint. */
    val Dashboard = 840.dp

    /** Smallest viewport that keeps two report tiles comfortably readable side-by-side. */
    val ReportTwoColumn = 360.dp
}

package com.autodrive.app.feature.home.domain.model

import com.autodrive.app.core.model.money.Money

// ═══════════════════════════════════════════════
// DynamoState — حالة محايدة داخل Domain
// أسماء العرض والصور تملكها Presentation.
// ═══════════════════════════════════════════════

enum class DynamoState { OFF, TIRED, AWAKE, FIRED_UP, LEGEND }

fun computeDynamoState(weeklyTotal: Money, myRank: Int?): DynamoState {
    val base = when {
        weeklyTotal <= Money.ZERO         -> DynamoState.OFF
        weeklyTotal <= Money.of(50_000L)  -> DynamoState.TIRED
        weeklyTotal <= Money.of(150_000L) -> DynamoState.AWAKE
        weeklyTotal <= Money.of(300_000L) -> DynamoState.FIRED_UP
        else                              -> DynamoState.LEGEND
    }
    return when (myRank) {
        1    -> base.boosted()
        2, 3 -> if (base >= DynamoState.AWAKE) base.boosted() else base
        else -> base
    }
}

private fun DynamoState.boosted(): DynamoState = when (this) {
    DynamoState.OFF      -> DynamoState.TIRED
    DynamoState.TIRED    -> DynamoState.AWAKE
    DynamoState.AWAKE    -> DynamoState.FIRED_UP
    DynamoState.FIRED_UP -> DynamoState.LEGEND
    DynamoState.LEGEND   -> DynamoState.LEGEND
}

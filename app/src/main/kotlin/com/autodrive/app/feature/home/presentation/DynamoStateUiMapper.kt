package com.autodrive.app.feature.home.presentation

import androidx.annotation.DrawableRes
import com.autodrive.app.R
import com.autodrive.app.feature.home.domain.model.DynamoState

fun DynamoState.arabicLabel(): String = when (this) {
    DynamoState.OFF      -> "مطفي"
    DynamoState.TIRED    -> "تعبان"
    DynamoState.AWAKE    -> "صاحي"
    DynamoState.FIRED_UP -> "مولع"
    DynamoState.LEGEND   -> "الأسطورة"
}

@DrawableRes
fun DynamoState.imageRes(): Int = when (this) {
    DynamoState.OFF      -> R.drawable.am_dynamo_off
    DynamoState.TIRED    -> R.drawable.am_dynamo_tired
    DynamoState.AWAKE    -> R.drawable.am_dynamo_awake
    DynamoState.FIRED_UP -> R.drawable.am_dynamo_fire
    DynamoState.LEGEND   -> R.drawable.am_dynamo_legend
}

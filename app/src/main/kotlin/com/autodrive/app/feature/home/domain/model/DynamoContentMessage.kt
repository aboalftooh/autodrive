package com.autodrive.app.feature.home.domain.model

data class DynamoContentMessage(
    val id: String,
    val contentType: String,
    val message: String,
)

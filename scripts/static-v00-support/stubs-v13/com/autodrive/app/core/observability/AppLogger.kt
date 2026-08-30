package com.autodrive.app.core.observability
object AppLogger {
    fun d(tag: String, message: String) = Unit
    fun w(tag: String, message: String) = Unit
    fun w(tag: String, message: String, fields: Map<String, Any?>) = Unit
    fun e(tag: String, message: String, error: Throwable?, fields: Map<String, Any?> = emptyMap()) = Unit
    fun event(tag: String, name: String, fields: Map<String, Any?> = emptyMap()) = Unit
}

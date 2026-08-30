package org.junit

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Test(val expected: KClass<out Throwable> = None::class) {
    class None : Throwable()
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Before

object Assert {
    @JvmStatic fun assertTrue(condition: Boolean) { if (!condition) throw AssertionError() }
    @JvmStatic fun assertTrue(message: String, condition: Boolean) { if (!condition) throw AssertionError(message) }
    @JvmStatic fun assertFalse(condition: Boolean) { if (condition) throw AssertionError() }
    @JvmStatic fun assertFalse(message: String, condition: Boolean) { if (condition) throw AssertionError(message) }
    @JvmStatic fun assertEquals(expected: Any?, actual: Any?) { if (expected != actual) throw AssertionError("expected=$expected actual=$actual") }
    @JvmStatic fun assertEquals(message: String, expected: Any?, actual: Any?) { if (expected != actual) throw AssertionError("$message expected=$expected actual=$actual") }
    @JvmStatic fun assertNull(actual: Any?) { if (actual != null) throw AssertionError("expected null actual=$actual") }
    @JvmStatic fun assertSame(expected: Any?, actual: Any?) { if (expected !== actual) throw AssertionError("expected same instance") }
    @JvmStatic fun <T: Throwable> assertThrows(type: Class<T>, block: () -> Unit): T {
        try { block() } catch (t: Throwable) { if (type.isInstance(t)) return type.cast(t); throw AssertionError("wrong exception ${t::class.java.name}", t) }
        throw AssertionError("expected exception ${type.name}")
    }
}

import org.junit.Before
import org.junit.Test

fun main() {
    val classes = listOf(
        "com.autodrive.app.core.model.money.MoneyTest",
        "com.autodrive.app.core.observability.SensitiveDataRedactorTest",
        "com.autodrive.app.core.session.domain.CurrentSessionTest",
        "com.autodrive.app.core.sync.data.DefaultSyncCoordinatorTest",
        "com.autodrive.app.core.sync.data.SyncStepExecutorTest",
        "com.autodrive.app.core.sync.outbox.OutboxRetryPolicyTest",
        "com.autodrive.app.core.sync.outbox.OutboxSensitiveErrorTest",
        "com.autodrive.app.core.sync.outbox.PendingOperationProcessorTest",
        "com.autodrive.app.core.sync.realtime.RealtimeEventPolicyTest",
        "com.autodrive.app.feature.auth.domain.validation.SudanPhoneNumberTest",
        "com.autodrive.app.feature.chat.data.ChatMediaErrorMapperTest",
        "com.autodrive.app.feature.commission.domain.CommissionCalculatorTest",
        "com.autodrive.app.feature.reports.domain.usecase.GetInvoiceDetailsUseCaseTest",
        "com.autodrive.app.navigation.NotificationDestinationResolverTest",
    )
    var passed = 0
    var total = 0
    classes.forEach { name ->
        val clazz = Class.forName(name)
        val before = clazz.declaredMethods.filter { it.getAnnotation(Before::class.java) != null }
        clazz.declaredMethods.filter { it.getAnnotation(Test::class.java) != null }.sortedBy { it.name }.forEach { method ->
            total++
            val instance = clazz.getDeclaredConstructor().newInstance()
            before.forEach { it.isAccessible = true; it.invoke(instance) }
            val expected = method.getAnnotation(Test::class.java).expected.java
            try {
                method.isAccessible = true
                method.invoke(instance)
                if (expected != Test.None::class.java) throw AssertionError("expected ${expected.name}")
                passed++
                println("PASS: ${clazz.simpleName}.${method.name}")
            } catch (wrapped: Throwable) {
                val cause = wrapped.cause ?: wrapped
                if (expected != Test.None::class.java && expected.isInstance(cause)) {
                    passed++
                    println("PASS: ${clazz.simpleName}.${method.name} (expected ${expected.simpleName})")
                } else {
                    println("FAIL: ${clazz.simpleName}.${method.name}: ${cause.message}")
                }
            }
        }
    }
    println("behavior tests: $passed/$total PASS")
    if (passed != total) error("Behavior test failure")
}

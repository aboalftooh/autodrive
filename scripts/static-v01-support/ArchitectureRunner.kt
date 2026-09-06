import org.junit.Test

fun main() {
    val classes = listOf(
        "com.autodrive.app.architecture.ClosureCleanupArchitectureTest",
        "com.autodrive.app.architecture.DatabaseSafetyArchitectureTest",
        "com.autodrive.app.architecture.DomainPresentationBoundaryArchitectureTest",
        "com.autodrive.app.architecture.FeatureOwnershipArchitectureTest",
        "com.autodrive.app.architecture.GradleModuleArchitectureTest",
        "com.autodrive.app.architecture.MoneyArchitectureTest",
        "com.autodrive.app.architecture.ObservabilitySecurityArchitectureTest",
        "com.autodrive.app.architecture.OutboxArchitectureTest",
        "com.autodrive.app.architecture.PackageByFeatureArchitectureTest",
        "com.autodrive.app.architecture.RealtimeArchitectureTest",
        "com.autodrive.app.architecture.ResponsibilitySplitArchitectureTest",
        "com.autodrive.app.architecture.RoomPerformanceArchitectureTest",
        "com.autodrive.app.architecture.SessionIsolationArchitectureTest",
        "com.autodrive.app.architecture.SyncBoundaryArchitectureTest"
    )
    var passed = 0
    var total = 0
    classes.forEach { name ->
        val clazz = Class.forName(name)
        val instance = clazz.getDeclaredConstructor().newInstance()
        clazz.declaredMethods.filter { it.getAnnotation(Test::class.java) != null }
            .sortedBy { it.name }
            .forEach { method ->
                total++
                try {
                    method.isAccessible = true
                    method.invoke(instance)
                    passed++
                    println("PASS: ${clazz.simpleName}.${method.name}")
                } catch (error: Throwable) {
                    val cause = error.cause ?: error
                    println("FAIL: ${clazz.simpleName}.${method.name}: ${cause.message}")
                }
            }
    }
    println("architecture reviews: $passed/$total PASS")
    if (passed != total) error("Architecture review failure")
}

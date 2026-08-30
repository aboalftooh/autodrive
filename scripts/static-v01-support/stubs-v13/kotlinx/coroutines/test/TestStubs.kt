package kotlinx.coroutines.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
fun runTest(block: suspend CoroutineScope.() -> Unit) = runBlocking { block(this) }
suspend fun CoroutineScope.runCurrent() { yield() }

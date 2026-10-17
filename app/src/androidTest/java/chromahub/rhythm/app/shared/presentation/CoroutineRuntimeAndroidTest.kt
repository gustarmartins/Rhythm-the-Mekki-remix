/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.shared.presentation

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Collections
import java.util.ServiceLoader
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Runs without an Activity so missing test services can be diagnosed before Compose starts. */
@RunWith(AndroidJUnit4::class)
class CoroutineRuntimeAndroidTest {
    @Test fun instrumentationDiscoversAndReportsUncaughtCoroutineFailures() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val service = CoroutineExceptionHandler::class.java
        val resource = "META-INF/services/${service.name}"
        val loaders = linkedMapOf(
            "coroutines" to service.classLoader,
            "test" to javaClass.classLoader,
            "target" to instrumentation.targetContext.classLoader,
            "instrumentation" to instrumentation.context.classLoader,
            "thread" to Thread.currentThread().contextClassLoader,
        )
        val providers = ServiceLoader.load(service, service.classLoader).toList()
        val details = buildString {
            loaders.forEach { (name, loader) ->
                appendLine("$name loader=$loader")
                if (loader != null) {
                    appendLine("$name resources=${Collections.list(loader.getResources(resource))}")
                }
            }
            appendLine("providers=${providers.map { it.javaClass.name }}")
            // Diagnostic only: never replace or mutate the coroutine runtime's handler list.
            val cached = runCatching {
                val owner = Class.forName("kotlinx.coroutines.internal.CoroutineExceptionHandlerImplKt")
                val getter = owner.declaredMethods.first {
                    it.name.startsWith("getPlatformExceptionHandlers") && it.parameterCount == 0
                }
                (getter.invoke(null) as Collection<*>).map { it?.javaClass?.name }
            }
            appendLine("cachedProviders=${cached.fold({ it.toString() }, { it.javaClass.simpleName })}")
        }
        instrumentation.sendStatus(0, Bundle().apply { putString("coroutine_runtime", details) })
        assertTrue(details, providers.any { it.javaClass.name.startsWith("kotlinx.coroutines.test.") })

        val marker = "rhythm-instrumentation-exception-collector"
        val failure = runCatching {
            runTest {
                val detachedScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
                try {
                    // This failure reaches the platform exception collector, rather than
                    // propagating through runTest's own parent/child Job relationship.
                    detachedScope.launch { throw IllegalStateException(marker) }.join()
                } finally {
                    detachedScope.cancel()
                }
            }
        }.exceptionOrNull()
        assertEquals(details, IllegalStateException::class.java, failure?.javaClass)
        assertEquals(details, marker, failure?.message)
    }
}

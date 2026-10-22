package chromahub.rhythm.app.util

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtworkColorRequestsTest {
    @Test fun returningToSameArtworkDoesNotReviveItsCancelledRequest() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined + Job())
        try {
            val firstDecode = CompletableDeferred<Unit>()
            var aLoads = 0
            val published = mutableListOf<String?>()
            val requests = ArtworkColorRequests(scope, { key ->
                val version = if (key == "A") ++aLoads else 1
                if (key == "A" && version == 1) {
                    withContext(NonCancellable) { firstDecode.await() }
                }
                "$key-$version"
            }, published::add)
            requests.submit("A")
            requests.submit("B")
            requests.submit("A")
            firstDecode.complete(Unit)
            scope.coroutineContext[Job]!!.children.toList().joinAll()
            assertEquals(listOf("B-1", "A-2"), published)
        } finally { scope.cancel() }
    }

    @Test fun olderDecodeCannotOverwriteNewSong() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined + Job())
        try {
            val oldDecode = CompletableDeferred<Unit>()
            val published = mutableListOf<String?>()
            val requests = ArtworkColorRequests(scope, { key ->
                if (key == "old") withContext(NonCancellable) { oldDecode.await() }
                key
            }, published::add)
            requests.submit("old")
            requests.submit("new")
            oldDecode.complete(Unit)
            scope.coroutineContext[Job]!!.children.toList().joinAll()
            assertEquals(listOf("new"), published)
        } finally { scope.cancel() }
    }

    @Test fun missingArtworkAndDisabledSourceInvalidatePendingDecode() = runBlocking {
        for (clear in listOf(true, false)) {
            val scope = CoroutineScope(Dispatchers.Unconfined + Job())
            try {
                val decode = CompletableDeferred<Unit>()
                val published = mutableListOf<String?>()
                val requests = ArtworkColorRequests(scope, {
                    withContext(NonCancellable) { decode.await() }
                    "old"
                }, published::add)
                requests.submit("old")
                if (clear) requests.submit(null) else requests.cancel()
                decode.complete(Unit)
                scope.coroutineContext[Job]!!.children.toList().joinAll()
                assertEquals(if (clear) listOf<String?>(null) else emptyList(), published)
            } finally { scope.cancel() }
        }
    }

    @Test fun sharesInFlightWorkButAllowsExplicitRefreshAfterCompletion() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined + Job())
        try {
            val decode = CompletableDeferred<Unit>()
            var loads = 0
            val published = mutableListOf<String?>()
            val requests = ArtworkColorRequests(scope, {
                loads++
                decode.await()
                "colors-$loads"
            }, published::add)
            requests.submit("same-uri")
            requests.submit("same-uri")
            assertEquals(1, loads)
            decode.complete(Unit)
            scope.coroutineContext[Job]!!.children.toList().joinAll()
            requests.submit("same-uri")
            scope.coroutineContext[Job]!!.children.toList().joinAll()
            assertEquals(2, loads)
            assertEquals(listOf("colors-1", "colors-2"), published)
        } finally { scope.cancel() }
    }
}

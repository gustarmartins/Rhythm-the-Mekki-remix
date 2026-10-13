package chromahub.rhythm.app.util

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.Process
import androidx.core.content.ContextCompat
import chromahub.rhythm.app.shared.data.model.AppSettings
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/** Process lifecycle and battery events govern optional enrichment; audio never enters this pool. */
object OptionalWorkController {
    private val gate = OptionalWorkGate()
    private val workerTids = ConcurrentHashMap.newKeySet<Int>()
    private val dispatcher = Executors.newFixedThreadPool(3) { runnable ->
        Thread({
            val tid = Process.myTid()
            workerTids.add(tid)
            Process.setThreadPriority(if (gate.policy.foreground) Process.THREAD_PRIORITY_DEFAULT else Process.THREAD_PRIORITY_BACKGROUND)
            try { runnable.run() } finally { workerTids.remove(tid) }
        }, "Rhythm-enrichment").apply { isDaemon = true }
    }.asCoroutineDispatcher()
    private val _foreground = MutableStateFlow(false)
    val foreground = _foreground.asStateFlow()
    private var initialized = false

    fun initialize(app: Application, settings: AppSettings) {
        if (initialized) return
        initialized = true
        var resumed = 0
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) { resumed++; setForeground(true) }
            override fun onActivityPaused(activity: Activity) { resumed = (resumed - 1).coerceAtLeast(0); setForeground(resumed > 0) }
            override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != Intent.ACTION_BATTERY_CHANGED) return
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val percent = if (level >= 0 && scale > 0) level * 100 / scale else null
                val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
                settings.updateBatteryState(percent, plugged)
            }
        }
        ContextCompat.registerReceiver(app, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        update(settings)
    }

    private fun setForeground(value: Boolean) {
        _foreground.value = value
        gate.update(gate.policy.copy(foreground = value))
        val priority = if (value) Process.THREAD_PRIORITY_DEFAULT else Process.THREAD_PRIORITY_BACKGROUND
        workerTids.forEach { tid -> runCatching { Process.setThreadPriority(tid, priority) } }
    }

    fun update(settings: AppSettings) {
        gate.update(OptionalWorkPolicy(_foreground.value, settings.batterySaverActive.value, buildSet {
            if (settings.batterySaverDisableAutoFetchArtwork.value) add(OptionalWork.ARTWORK)
            if (settings.batterySaverDisableLyricsFetch.value) add(OptionalWork.LYRICS)
            if (settings.batterySaverDisableLibraryWork.value) add(OptionalWork.LIBRARY)
            if (settings.batterySaverDisableCanvas.value) add(OptionalWork.CANVAS)
        }))
    }

    suspend fun <T> run(kind: OptionalWork, block: suspend () -> T): T =
        gate.run(kind) { withContext(dispatcher) { block() } }
}

package chromahub.rhythm.app.util

/**
 * Separates explicit app commands from Media3's internal null-action wakeups.
 *
 * Explicit commands can arrive through startForegroundService() and therefore
 * need the service to satisfy the foreground contract immediately. Media3
 * already owns foreground notification state for its null-action wakeups; a
 * second placeholder promotion on every metadata change duplicates
 * notifications and needlessly restarts foreground-service bookkeeping.
 */
internal object ServiceStartPolicy {
    fun requiresManualForegroundPromotion(action: String?): Boolean = action != null
}

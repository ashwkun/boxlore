package cx.aswin.boxcast.core.designsystem.performance

import android.util.Log
import android.view.Window
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.PerformanceMetricsState

class JankTracker(window: Window, private val tag: String = "JankTracker") {
    
    private val metricsStateHolder = PerformanceMetricsState.getHolderForHierarchy(window.decorView)
    
    private val jankStats = JankStats.createAndTrack(window) { frameData ->
        if (frameData.isJank) {
            val states = frameData.states.joinToString { "${it.key}=${it.value}" }
            val durationMs = frameData.frameDurationUiNanos / 1_000_000
            Log.w(tag, "Jank Frame! Duration: ${durationMs}ms (Limit: 16.6ms), States: [$states]")
        }
    }

    fun updateUiState(key: String, value: String) {
        metricsStateHolder.state?.putState(key, value)
    }

    fun clearUiState(key: String) {
        metricsStateHolder.state?.removeState(key)
    }

    fun startTracking() {
        jankStats.isTrackingEnabled = true
    }

    fun stopTracking() {
        jankStats.isTrackingEnabled = false
    }
}

val LocalJankTracker = staticCompositionLocalOf<JankTracker?> { null }

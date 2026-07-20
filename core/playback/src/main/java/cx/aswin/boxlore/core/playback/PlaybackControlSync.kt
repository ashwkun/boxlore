package cx.aswin.boxlore.core.playback

/**
 * Keeps player UI transport controls aligned with the live Media3 player / prefs
 * when [PlayerState] is cleared or a new queue starts.
 *
 * Clearing to a blank [PlayerState] used to reset [PlayerState.playbackSpeed] to 1×
 * while ExoPlayer kept the persisted rate — the UI then showed 1× while audio ran at 2×.
 */
object PlaybackControlSync {
    /**
     * Empty session state after dismiss/clear that preserves speed and seek sizes.
     */
    fun clearedStatePreservingControls(
        previous: PlayerState,
        controllerSpeed: Float?,
    ): PlayerState =
        PlayerState(
            playbackSpeed = resolvePlaybackSpeed(controllerSpeed, previous.playbackSpeed),
            seekBackwardMs = previous.seekBackwardMs,
            seekForwardMs = previous.seekForwardMs,
        )

    /**
     * Prefer the live controller rate, then in-memory state, then 1×.
     */
    fun resolvePlaybackSpeed(
        controllerSpeed: Float?,
        stateSpeed: Float,
        fallback: Float = 1.0f,
    ): Float {
        val fromController = controllerSpeed?.takeIf { it.isFinite() && it > 0f }
        if (fromController != null) return fromController
        return stateSpeed.takeIf { it.isFinite() && it > 0f } ?: fallback
    }
}

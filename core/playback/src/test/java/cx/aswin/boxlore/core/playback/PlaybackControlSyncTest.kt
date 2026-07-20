package cx.aswin.boxlore.core.playback

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlaybackControlSyncTest {
    @Test
    fun resolvePlaybackSpeedPrefersController() {
        assertEquals(
            2.0f,
            PlaybackControlSync.resolvePlaybackSpeed(
                controllerSpeed = 2.0f,
                stateSpeed = 1.0f,
            ),
            0.0001f,
        )
    }

    @Test
    fun resolvePlaybackSpeedFallsBackToStateWhenControllerMissing() {
        assertEquals(
            1.75f,
            PlaybackControlSync.resolvePlaybackSpeed(
                controllerSpeed = null,
                stateSpeed = 1.75f,
            ),
            0.0001f,
        )
    }

    @Test
    fun resolvePlaybackSpeedIgnoresNonPositiveControllerSpeed() {
        assertEquals(
            1.5f,
            PlaybackControlSync.resolvePlaybackSpeed(
                controllerSpeed = 0f,
                stateSpeed = 1.5f,
            ),
            0.0001f,
        )
    }

    @Test
    fun clearedStatePreservesSpeedAndSeekControls() {
        val previous =
            PlayerState(
                playbackSpeed = 1.0f,
                seekBackwardMs = 15_000L,
                seekForwardMs = 45_000L,
                currentEpisode = null,
            )

        val cleared =
            PlaybackControlSync.clearedStatePreservingControls(
                previous = previous,
                controllerSpeed = 2.0f,
            )

        assertEquals(2.0f, cleared.playbackSpeed, 0.0001f)
        assertEquals(15_000L, cleared.seekBackwardMs)
        assertEquals(45_000L, cleared.seekForwardMs)
        assertEquals(null, cleared.currentEpisode)
        assertEquals(false, cleared.isPlaying)
    }

    @Test
    fun clearedStateUsesPreviousSpeedWhenControllerUnavailable() {
        val previous = PlayerState(playbackSpeed = 1.25f)

        val cleared =
            PlaybackControlSync.clearedStatePreservingControls(
                previous = previous,
                controllerSpeed = null,
            )

        assertEquals(1.25f, cleared.playbackSpeed, 0.0001f)
    }
}

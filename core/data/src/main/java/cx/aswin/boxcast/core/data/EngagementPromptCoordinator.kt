package cx.aswin.boxcast.core.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Coordinates proactive engagement prompts (NPS survey, Play review) so only one
 * surfaces per session and promoter handoffs follow the unified strategy.
 */
class EngagementPromptCoordinator(
    private val userPrefs: UserPreferencesRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    /** In-memory guard: at most one proactive modal per app process session. */
    @Volatile
    var sessionProactivePromptShown: Boolean = false
        private set

    fun canShowProactivePrompt(isPlaying: Boolean): Boolean =
        !isPlaying && !sessionProactivePromptShown

    fun recordProactivePromptShown() {
        sessionProactivePromptShown = true
        scope.launch { userPrefs.recordEngagementPromptShown() }
    }

    fun onSurveyDisplayed() {
        recordProactivePromptShown()
    }

    fun onNpsRatingSubmitted(score: Int?) {
        if (score == null) return
        scope.launch {
            userPrefs.setNpsLastScore(score)
            if (score >= PROMOTER_SCORE_THRESHOLD) {
                userPrefs.setPromoterReviewPending(true)
            }
        }
    }

    suspend fun shouldShowPromoterReview(isPlaying: Boolean): Boolean {
        if (!canShowProactivePrompt(isPlaying)) return false
        if (userPrefs.hasReviewedSync()) return false
        if (!userPrefs.hasNpsSurveyFired()) return false
        return userPrefs.isPromoterReviewPending()
    }

    suspend fun clearPromoterReviewPending() {
        userPrefs.setPromoterReviewPending(false)
    }

    suspend fun isEngagementCooldownElapsed(): Boolean = userPrefs.isEngagementCooldownElapsed()

    companion object {
        const val PROMOTER_SCORE_THRESHOLD = 8
        const val ENGAGEMENT_COOLDOWN_DAYS = 14
        const val DETRACTOR_SCORE_MAX = 7
    }
}

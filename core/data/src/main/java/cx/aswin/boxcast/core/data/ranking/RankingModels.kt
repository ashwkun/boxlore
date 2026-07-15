package cx.aswin.boxcast.core.data.ranking

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

enum class RankingObjective(
    val allowsExploration: Boolean,
) {
    YOUR_SHOWS(allowsExploration = false),
    DISCOVERY(allowsExploration = true),
    CONTINUATION(allowsExploration = true),
    OFFLINE(allowsExploration = false),
    SLATE(allowsExploration = true),
}

enum class RankingSurface {
    HOME,
    EXPLORE,
    LIBRARY,
    QUEUE,
    DOWNLOADS,
    ANDROID_AUTO,
}

enum class CandidateSource {
    SUBSCRIPTION,
    LOCAL_HISTORY,
    SERVER_RECOMMENDATION,
    CURATED_INTENT,
    TRENDING,
    LIKED,
    DOWNLOADED,
}

enum class FeatureSlot {
    INTERCEPT,
    SHOW_AFFINITY,
    GENRE_AFFINITY,
    SOURCE_AFFINITY,
    FRESHNESS,
    NOVELTY,
    DURATION_FIT,
    SUBSCRIBED,
    RESUME_PROGRESS,
    UNPLAYED,
    SERIAL_MATCH,
    SERVER_RELEVANCE,
    EXPOSURE_FATIGUE,
    TIME_CONTEXT,
    OFFLINE_SUITABILITY,
    EXPLICIT_PREFERENCE,
    RECENT_SUBSCRIPTION,
    CURRENT_SHOW,
}

object RankingFeatureSchema {
    const val VERSION = 1
    val dimension: Int = FeatureSlot.entries.size
}

data class CandidateSignals(
    val showAffinity: Double = 0.0,
    val genreAffinity: Double = 0.0,
    val sourceAffinity: Double = 0.0,
    val ageHours: Double? = null,
    val isUnseenShow: Boolean = false,
    val durationFit: Double = 0.5,
    val isSubscribed: Boolean = false,
    val progressRatio: Double = 0.0,
    val isUnplayed: Boolean = true,
    val serialMatch: Double = 0.5,
    val serverRelevance: Double = 0.0,
    val recentExposureCount: Int = 0,
    val timeContextMatch: Double = 0.5,
    val offlineSuitability: Double = 0.5,
    val explicitPreference: Double = 0.0,
    val hoursSinceSubscription: Double? = null,
    val isCurrentShow: Boolean = false,
)

data class RankingFeatures(
    val schemaVersion: Int = RankingFeatureSchema.VERSION,
    val values: DoubleArray,
) {
    init {
        require(values.size == RankingFeatureSchema.dimension) {
            "Expected ${RankingFeatureSchema.dimension} ranking features, got ${values.size}"
        }
        require(values.all(Double::isFinite)) { "Ranking features must be finite" }
    }
}

object CandidateFeatureBuilder {
    fun build(signals: CandidateSignals): RankingFeatures {
        val values = DoubleArray(RankingFeatureSchema.dimension)
        values[FeatureSlot.INTERCEPT.ordinal] = 1.0
        values[FeatureSlot.SHOW_AFFINITY.ordinal] = signals.showAffinity.unit()
        values[FeatureSlot.GENRE_AFFINITY.ordinal] = signals.genreAffinity.unit()
        values[FeatureSlot.SOURCE_AFFINITY.ordinal] = signals.sourceAffinity.unit()
        values[FeatureSlot.FRESHNESS.ordinal] = signals.ageHours
            ?.coerceAtLeast(0.0)
            ?.let { exp(-it / (24.0 * 14.0)) }
            ?: 0.0
        values[FeatureSlot.NOVELTY.ordinal] = signals.isUnseenShow.asUnit()
        values[FeatureSlot.DURATION_FIT.ordinal] = signals.durationFit.unit()
        values[FeatureSlot.SUBSCRIBED.ordinal] = signals.isSubscribed.asUnit()
        values[FeatureSlot.RESUME_PROGRESS.ordinal] = signals.progressRatio.unit()
        values[FeatureSlot.UNPLAYED.ordinal] = signals.isUnplayed.asUnit()
        values[FeatureSlot.SERIAL_MATCH.ordinal] = signals.serialMatch.unit()
        values[FeatureSlot.SERVER_RELEVANCE.ordinal] = signals.serverRelevance.unit()
        values[FeatureSlot.EXPOSURE_FATIGUE.ordinal] =
            -(1.0 - exp(-signals.recentExposureCount.coerceAtLeast(0) / 3.0))
        values[FeatureSlot.TIME_CONTEXT.ordinal] = signals.timeContextMatch.unit()
        values[FeatureSlot.OFFLINE_SUITABILITY.ordinal] = signals.offlineSuitability.unit()
        values[FeatureSlot.EXPLICIT_PREFERENCE.ordinal] = signals.explicitPreference.coerceIn(-1.0, 1.0)
        values[FeatureSlot.RECENT_SUBSCRIPTION.ordinal] = signals.hoursSinceSubscription
            ?.coerceAtLeast(0.0)
            ?.let { exp(-it / (24.0 * 14.0)) }
            ?: 0.0
        values[FeatureSlot.CURRENT_SHOW.ordinal] = signals.isCurrentShow.asUnit()
        return RankingFeatures(values = values)
    }
}

data class RankingScore(
    val finalScore: Double,
    val priorScore: Double,
    val learnedScore: Double,
    val explorationBonus: Double,
    val learnedBlend: Double,
    val updateCount: Long,
    val contributions: Map<FeatureSlot, Double>,
)

data class RankedCandidate<T>(
    val value: T,
    val episodeId: String,
    val podcastId: String,
    val genre: String?,
    val score: Double,
    val isNovel: Boolean = false,
)

data class DiversityPolicy(
    val limit: Int,
    val maxPerShow: Int = 2,
    val genreRepeatPenalty: Double = 0.08,
    val recentPodcastIds: Set<String> = emptySet(),
    val recentShowPenalty: Double = 0.12,
    val reserveNovelSlot: Boolean = false,
)

object DiversityReranker {
    fun <T> rerank(
        candidates: List<RankedCandidate<T>>,
        policy: DiversityPolicy,
    ): List<RankedCandidate<T>> {
        if (policy.limit <= 0) return emptyList()
        val remaining = candidates.distinctBy { it.episodeId }.toMutableList()
        val selected = mutableListOf<RankedCandidate<T>>()
        val showCounts = mutableMapOf<String, Int>()
        val genreCounts = mutableMapOf<String, Int>()

        while (selected.size < policy.limit && remaining.isNotEmpty()) {
            val best = remaining
                .asSequence()
                .filter { (showCounts[it.podcastId] ?: 0) < policy.maxPerShow }
                .maxByOrNull { candidate ->
                    val normalizedGenre = candidate.genre?.trim()?.lowercase().orEmpty()
                    val genrePenalty = (genreCounts[normalizedGenre] ?: 0) * policy.genreRepeatPenalty
                    val recentPenalty = if (candidate.podcastId in policy.recentPodcastIds) {
                        policy.recentShowPenalty
                    } else {
                        0.0
                    }
                    candidate.score - genrePenalty - recentPenalty
                }
                ?: break
            selected += best
            remaining.remove(best)
            showCounts[best.podcastId] = (showCounts[best.podcastId] ?: 0) + 1
            val genre = best.genre?.trim()?.lowercase().orEmpty()
            if (genre.isNotEmpty()) genreCounts[genre] = (genreCounts[genre] ?: 0) + 1
        }

        if (policy.reserveNovelSlot && selected.none { it.isNovel }) {
            val novel = candidates
                .asSequence()
                .filter { it.isNovel && selected.none { selectedItem -> selectedItem.episodeId == it.episodeId } }
                .filter { (showCounts[it.podcastId] ?: 0) < policy.maxPerShow }
                .maxByOrNull { it.score }
            if (novel != null) {
                if (selected.size >= policy.limit) selected.removeAt(selected.lastIndex)
                selected += novel
            }
        }
        return selected
    }
}

private fun Double.unit(): Double = min(1.0, max(0.0, this))

private fun Boolean.asUnit(): Double = if (this) 1.0 else 0.0

package cx.aswin.boxlore.core.catalog.home

import cx.aswin.boxlore.core.network.model.HomeCandidateItemDto
import cx.aswin.boxlore.core.network.model.HomeCandidateSeedDto
import cx.aswin.boxlore.core.network.model.HomeCandidateSeedFallbackDto
import cx.aswin.boxlore.core.network.model.HomeCandidatesV1Request
import cx.aswin.boxlore.core.network.model.HomeCandidatesV1Response
import cx.aswin.boxlore.core.network.model.HistoryItem

/**
 * Builds bounded Home candidate requests and maps module pools for the
 * Android-owned slate allocator. Network I/O stays on [PodcastRepository].
 */
object HomeCandidatesRequestBuilder {
    const val CACHE_TTL_MILLIS: Long = 4L * 60L * 60L * 1_000L

    data class BuildInput(
        val modules: List<String>,
        val country: String,
        val languages: List<String>,
        val history: List<HistoryItem>,
        val anchorPodcastId: String?,
        val missionId: String?,
        val excludedPodcastIds: List<String>,
        val excludedEpisodeIds: List<String>,
        val noveltyPreference: Double?,
        val daypart: String?,
        val revision: String?,
    )

    fun build(input: BuildInput): HomeCandidatesV1Request {
        return HomeCandidatesV1Request(
            requestedModules = input.modules.distinct().take(4),
            country = input.country.lowercase().takeIf { it.length in 2..3 } ?: "us",
            languages = input.languages,
            seeds = buildSeeds(input.history),
            anchorPodcastId = input.anchorPodcastId,
            missionId = input.missionId,
            excludedPodcastIds = input.excludedPodcastIds.distinct().take(250),
            excludedEpisodeIds = input.excludedEpisodeIds.distinct().take(250),
            noveltyPreference = input.noveltyPreference,
            daypart = input.daypart,
            revision = input.revision,
        )
    }

    fun buildSeeds(
        history: List<HistoryItem>,
        maximumSeeds: Int = 12,
    ): List<HomeCandidateSeedDto> {
        return history.mapNotNull { item ->
            val episodeId = item.episodeId?.takeIf { it.isNotBlank() }
            val podcastId = item.podcastId?.takeIf { it.isNotBlank() }
            if (episodeId == null && podcastId == null) return@mapNotNull null
            val durationMs = item.durationMs ?: 0L
            val progressRatio = if (durationMs > 0L) {
                (item.progressMs ?: 0L).toDouble() / durationMs
            } else {
                0.0
            }
            val weight = when {
                item.isLiked == true -> 0.95
                item.isCompleted == true -> 0.9
                progressRatio >= 0.5 -> 0.75
                progressRatio >= 0.2 -> 0.55
                else -> 0.35
            }
            HomeCandidateSeedDto(
                episodeId = episodeId,
                podcastId = podcastId,
                weight = weight,
                fallback = HomeCandidateSeedFallbackDto(
                    episodeTitle = item.episodeTitle.take(180),
                    podcastTitle = item.podcastTitle.take(180),
                    description = item.episodeDescription?.take(400),
                ),
            )
        }
            .sortedByDescending { it.weight }
            .distinctBy { it.episodeId ?: "p:${it.podcastId}" }
            .take(maximumSeeds)
    }

    fun moduleItems(
        response: HomeCandidatesV1Response,
        module: String,
    ): List<HomeCandidateItemDto> {
        return when (module) {
            "taste" -> response.modules.taste
            "because_you_like" -> response.modules.becauseYouLike
            "mission" -> response.modules.mission
            "regional" -> response.modules.regional
            else -> emptyList()
        }
    }

    fun isValid(response: HomeCandidatesV1Response): Boolean {
        return response.status == "true" &&
            response.contractVersion == 1 &&
            !response.algorithmVersion.isNullOrBlank()
    }
}

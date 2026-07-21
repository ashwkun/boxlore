package cx.aswin.boxlore.core.playback.service

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import cx.aswin.boxlore.core.database.BoxLoreDatabase
import cx.aswin.boxlore.core.database.DownloadedEpisodeEntity
import cx.aswin.boxlore.core.database.ListeningHistoryEntity
import cx.aswin.boxlore.core.database.PodcastEntity
import cx.aswin.boxlore.core.model.Episode
import cx.aswin.boxlore.core.model.Podcast
import cx.aswin.boxlore.core.playback.MixtapeEngine
import cx.aswin.boxlore.core.playback.QueueRepository
import cx.aswin.boxlore.core.playback.SmartQueueSources
import cx.aswin.boxlore.core.playback.service.auto.AutoBrowseContract
import cx.aswin.boxlore.core.ranking.AdaptiveCandidateScorer
import cx.aswin.boxlore.core.ranking.RankingSurface
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicLong

/**
 * Prewarms Android Auto folder collage artwork for [BoxLorePlaybackService].
 */
internal class AutoCollagePrewarmer(
    private val context: Context,
    private val database: BoxLoreDatabase,
    private val queueRepository: QueueRepository,
    private val smartQueueSources: SmartQueueSources,
    private val adaptiveCandidateScorer: AdaptiveCandidateScorer,
    private val toAutoPodcast: (PodcastEntity) -> Podcast,
    private val mediaSessionProvider: () -> MediaLibrarySession?,
    private val onCollagesReady: (Map<String, Uri>) -> Unit,
) {
    private val mutex = Mutex()
    private val lastPrewarmAtMs = AtomicLong(0L)

    suspend fun prewarm(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastPrewarmAtMs.get() < MIN_REFRESH_INTERVAL_MS) {
            return
        }
        mutex.withLock {
            val lockedNow = System.currentTimeMillis()
            if (!force && lockedNow - lastPrewarmAtMs.get() < MIN_REFRESH_INTERVAL_MS) {
                return
            }
            runPrewarm()
            lastPrewarmAtMs.set(System.currentTimeMillis())
        }
    }

    private suspend fun runPrewarm() {
        try {
            val snapshot = loadSnapshot()
            val mixtape = resolveMixtape(snapshot)
            val uris =
                AutoCollageGenerator.generateAllCollages(
                    context = context,
                    folderImages = snapshot.folderImages(mixtape),
                    folderContentKeys = snapshot.folderContentKeys(mixtape),
                )
            onCollagesReady(uris)
            notifyBrowseTree(snapshot.subscriptions.size, snapshot.resumeItems.size)
        } catch (error: Exception) {
            Log.w("AutoBrowse", "Unable to prewarm Android Auto artwork", error)
        }
    }

    private suspend fun loadSnapshot(): PrewarmSnapshot {
        val history = database.listeningHistoryDao().getRecentHistoryList(300)
        val resumeItems = database.listeningHistoryDao().getResumeItemsList()
        val subscriptions = database.podcastDao().getSubscribedPodcastsList()
        val downloads = database.downloadedEpisodeDao().getCompletedDownloads(8)
        val queue = queueRepository.getQueueSnapshot()
        return PrewarmSnapshot(
            history = history,
            resumeItems = resumeItems,
            subscriptions = subscriptions,
            downloads = downloads,
            queue = queue,
        )
    }

    private suspend fun resolveMixtape(snapshot: PrewarmSnapshot): MixtapeEngine.Result {
        var mixtape =
            MixtapeEngine.build(
                subscriptions = snapshot.subscriptions.map(toAutoPodcast),
                history = snapshot.history,
                adaptiveRanking =
                    MixtapeEngine.AdaptiveRanking(
                        scorer = adaptiveCandidateScorer,
                        surface = RankingSurface.ANDROID_AUTO,
                    ),
            )
        if (mixtape.episodes.size >= 3) return mixtape
        val recommendations =
            runCatching {
                withTimeout(6_000L) {
                    smartQueueSources.getPersonalizedRecommendations(
                        history = smartQueueSources.getHistoryForRecommendations(25),
                        interests = smartQueueSources.getInterests(),
                        country = smartQueueSources.getRegion(),
                        subscribedPodcastIds = snapshot.subscriptions.map { it.podcastId },
                        subscribedGenres =
                            snapshot.subscriptions.mapNotNull { it.genre }.distinct(),
                    )
                }
            }.getOrDefault(emptyList())
        return MixtapeEngine.build(
            subscriptions = snapshot.subscriptions.map(toAutoPodcast),
            history = snapshot.history,
            recommendations = recommendations,
            adaptiveRanking =
                MixtapeEngine.AdaptiveRanking(
                    scorer = adaptiveCandidateScorer,
                    surface = RankingSurface.ANDROID_AUTO,
                ),
        )
    }

    private fun notifyBrowseTree(
        subscriptionCount: Int,
        resumeCount: Int,
    ) {
        val session = mediaSessionProvider()
        session?.notifyChildrenChanged(AutoBrowseContract.ROOT_ID, 4, null)
        session?.notifyChildrenChanged(AutoBrowseContract.HOME_ID, 3, null)
        session?.notifyChildrenChanged(
            AutoBrowseContract.LIBRARY_ID,
            subscriptionCount + 2,
            null,
        )
        session?.notifyChildrenChanged(AutoBrowseContract.DISCOVER_ID, 3, null)
        session?.notifyChildrenChanged(AutoBrowseContract.HOME_CONTINUE_ID, resumeCount, null)
    }

    private data class PrewarmSnapshot(
        val history: List<ListeningHistoryEntity>,
        val resumeItems: List<ListeningHistoryEntity>,
        val subscriptions: List<PodcastEntity>,
        val downloads: List<DownloadedEpisodeEntity>,
        val queue: List<Episode>,
    ) {
        private val historyImages =
            history.mapNotNull { it.episodeImageUrl ?: it.podcastImageUrl }
        private val resumeImages =
            resumeItems.mapNotNull { it.episodeImageUrl ?: it.podcastImageUrl }
        private val subscriptionImages = subscriptions.mapNotNull { it.imageUrl }
        private val downloadImages =
            downloads.mapNotNull { it.episodeImageUrl ?: it.podcastImageUrl }
        private val queueImages = queue.mapNotNull { it.imageUrl ?: it.podcastImageUrl }
        private val newEpisodeImages =
            subscriptions.mapNotNull { it.latestEpisode?.imageUrl ?: it.imageUrl }
        private val newEpisodeKeys =
            subscriptions.mapNotNull { podcast ->
                podcast.latestEpisode?.id ?: podcast.podcastId.takeIf {
                    podcast.latestEpisode != null || !podcast.imageUrl.isNullOrBlank()
                }
            }

        fun folderImages(mixtape: MixtapeEngine.Result): Map<String, List<String>> {
            val mixtapeImages =
                mixtape.podcasts.mapNotNull { podcast ->
                    podcast.latestEpisode?.let { episode ->
                        episode.imageUrl ?: episode.podcastImageUrl ?: podcast.imageUrl
                    }
                }
            return mapOf(
                AutoBrowseContract.HOME_ID to (historyImages + newEpisodeImages).take(4),
                AutoBrowseContract.LIBRARY_ID to subscriptionImages.take(4),
                AutoBrowseContract.DOWNLOADS_ID to downloadImages.take(4),
                AutoBrowseContract.DISCOVER_ID to subscriptionImages.asReversed().take(4),
                AutoBrowseContract.HOME_CONTINUE_ID to resumeImages.take(4),
                AutoBrowseContract.HOME_QUEUE_ID to queueImages.take(4),
                AutoBrowseContract.HOME_NEW_EPISODES_ID to newEpisodeImages.take(4),
                AutoBrowseContract.HOME_DRIVE_MIX_ID to mixtapeImages.take(4),
                AutoBrowseContract.DISCOVER_DRIVE_PICKS_ID to
                    (queueImages + subscriptionImages).take(4),
                AutoBrowseContract.DISCOVER_TIME_PICKS_ID to emptyList(),
                AutoBrowseContract.DISCOVER_GENRES_ID to emptyList(),
            )
        }

        fun folderContentKeys(mixtape: MixtapeEngine.Result): Map<String, List<String>> =
            mapOf(
                AutoBrowseContract.HOME_ID to history.take(4).map { it.episodeId },
                AutoBrowseContract.LIBRARY_ID to subscriptions.take(4).map { it.podcastId },
                AutoBrowseContract.DOWNLOADS_ID to downloads.map { it.episodeId },
                AutoBrowseContract.DISCOVER_ID to
                    subscriptions.asReversed().take(4).map { it.podcastId },
                AutoBrowseContract.HOME_CONTINUE_ID to resumeItems.map { it.episodeId },
                AutoBrowseContract.HOME_QUEUE_ID to queue.map { it.id },
                AutoBrowseContract.HOME_NEW_EPISODES_ID to newEpisodeKeys.take(4),
                AutoBrowseContract.HOME_DRIVE_MIX_ID to mixtape.episodes.map { it.id },
                AutoBrowseContract.DISCOVER_DRIVE_PICKS_ID to
                    (queue.map { it.id } + subscriptions.map { it.podcastId }).take(4),
                AutoBrowseContract.DISCOVER_TIME_PICKS_ID to
                    listOf(AutoBrowseContract.DISCOVER_TIME_PICKS_ID),
                AutoBrowseContract.DISCOVER_GENRES_ID to
                    listOf(AutoBrowseContract.DISCOVER_GENRES_ID),
            )
    }

    companion object {
        private const val MIN_REFRESH_INTERVAL_MS = 30_000L
    }
}

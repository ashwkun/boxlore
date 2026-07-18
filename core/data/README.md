# `:core:data`

## Purpose

**Catalog and orchestration layer.** Owns the Podcast Index catalog (`PodcastRepository`), subscriptions, smart queue logic, content sections, cross-promo, engagement, backup/restore, and the shared-deps composition bridge for workers and Media3 services. It is **not** a junk drawer — every type here is a catalog, subscription, or orchestration concern.

Extracted subsystems now live in dedicated modules (all re-exported via `api` so existing import paths continue to compile without callers adding a direct dependency):

| Subsystem | Module |
| :--- | :--- |
| RSS feed fetch / parse / IDs / `RssPodcastRepository` | `:core:rss` |
| Adaptive ranking / LinUCB / feedback loop | `:core:ranking` |
| Analytics façade (`AnalyticsHelper`, `Analytics`) | `:core:analytics` |
| Download worker stack | `:core:downloads` |
| Playback / queue / Media3 services | `:core:playback` |
| DataStore + SharedPrefs | `:core:prefs` |
| Main Room database | `:core:database` |

## Public API

- **Catalog:** `PodcastRepository` (Podcast Index HTTP + RSS delegate via `RssPodcastRepository`), `SubscriptionRepository`, `ChapterRepository`, `TranscriptRepository`
- **Smart queue helpers:** `QueueMath`, `QueueSkipMemory`, `SmartQueueEngine` / `SmartQueueSources`, `MixtapeEngine`
- **Content sections:** `content/ContentOrchestrator`, `content/GroupedContentSectionProvider`, `content/ContentContextEngine`
- **Backup/restore:** `backup/LibraryBackupManager` — JSON + OPML export/import, ranking backup, RSS re-import
- **Shared-deps bridge:**
  - `SharedAppDependencies` — interface of Application-scoped instances (DB, repositories, ranking, RSS, history source) consumed by workers/services via `SharedAppDependenciesHolder`
  - `SharedAppDependenciesHolder` — `@Volatile` install + `require()` (throws if unset)
- **Re-exported (api) subsystems:** all types from `:core:rss`, `:core:analytics`, `:core:ranking`, `:core:domain`, `:core:database`, `:core:prefs`
- **Data-only ports:** `ports.ListeningHistoryBackupPort`, `ports.SmartDownloadSyncPort`

> `ports.DownloadCacheRelinker` moved to `:core:rss` (same package `cx.aswin.boxlore.core.data.ports`; re-exported transitively through the `api(core:rss)` chain).

## Internal structure

```text
src/main/java/cx/aswin/boxlore/core/data/
  PodcastRepository.kt           # Podcast Index HTTP catalog + RSS delegate
  SubscriptionRepository.kt      # subscribe/unsubscribe, FCM topic management
  ChapterRepository.kt
  TranscriptRepository.kt
  EpisodeMapper.kt
  SharedAppDependencies.kt       # interface + holder for workers/services
  SmartQueueEngine.kt
  SmartQueueSources.kt
  QueueMath.kt
  QueueSkipMemory.kt
  MixtapeEngine.kt
  EngagementPromptCoordinator.kt
  InstallReferrerManager.kt
  content/                       # Personalised home sections (candidates, signals, cache policy)
  crosspromo/                    # Cross-promotion detection / resolution
  backup/                        # LibraryBackupManager (JSON + OPML)
  privacy/                       # ConsentManager
  ports/                         # SmartDownloadSyncPort, ListeningHistoryBackupPort
```

## Dependencies

- → `:core:rss` (`api` — re-exports all RSS types: `RssFeedClient`, `RssPodcastRepository`, `RssIdGenerator`, `RssSourceMatcher`, `DownloadCacheRelinker`, …)
- → `:core:analytics` (`api` — re-exports analytics façade)
- → `:core:ranking` (`api` — re-exports `AdaptiveCandidateScorer`, `RankingFeedbackRepository`, `AdaptiveRankingRepository`)
- → `:core:domain` (`api`), `:core:prefs` (`api`), `:core:database` (`api`)
- → `:core:model`, `:core:network` (internal)
- → Firebase (database + messaging — `SubscriptionRepository` uses both)
- → Retrofit, OkHttp, Gson, DataStore (internal)

Forbidden: `:core:data` **must not** depend on `:core:playback`, `:core:designsystem`, or `:core:downloads`.

## Threading / lifecycle

- Repositories are Application-scoped when obtained from `SharedAppDependenciesHolder` / `AppContainer`.
- Workers must not construct a second Podcast/ranking/RSS graph — obtain instances from the holder.

## Persistence & identity

| Stable | Why |
| :--- | :--- |
| DataStore `user_preferences` / SharedPrefs `boxcast_prefs` (owned by `:core:prefs`) | Existing installs |
| Main Room DB filename (owned by `:core:database`) | User data |
| `rss:` podcast IDs and negative episode IDs (owned by `:core:rss`) | Catalog identity |

## Testing notes

JVM unit tests under `src/test` cover catalog logic, content orchestration, queue math, smart queue, and the composition holder:

- `QueueMathTest`, `QueueSkipMemoryTest` — pure math
- `SmartQueueEngineTest` — recommendation ordering
- `content/ContentOrchestratorTest`, `content/ContentSignalEnrichmentTest`, `content/GroupedContentSectionsTest`, `content/RecentSectionIntentStoreTest`
- `crosspromo/CrossPromotionDetectorTest`
- `TranscriptRepositoryTest`
- `SharedAppDependenciesHolderTest` — `require()` throws when unset

RSS-specific tests (`RssIdGeneratorTest`, `RssSourceMatcherTest`) now live in `:core:rss`.

```bash
./gradlew :core:data:testDebugUnitTest
```

## CI relevance

Exercised by the unit-test CI job. Kover `merged` variant is added to the project-level coverage report.

## See also

- Root [`ARCHITECTURE.md`](../../ARCHITECTURE.md)
- [`:core:rss` README](../rss/README.md) — RSS feed client, ID generation, `RssPodcastRepository`
- [`:core:ranking` README](../ranking/README.md)
- [`:core:downloads` README](../downloads/README.md)
- [`:core:domain` README](../domain/README.md)
- [`:core:playback` README](../playback/README.md)
- [`docs/PLAN_MODULAR_ANDROID_HARDENING.md`](../../docs/PLAN_MODULAR_ANDROID_HARDENING.md) (Phase A6a)

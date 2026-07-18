# `:core:rss`

## Purpose

Owns the complete RSS podcast stack: feed fetching, parsing, ID generation, episode catalog management, and the RSS subscription port implementation. Deliberately does **not** own download management (lives in `:core:downloads`) or the Podcast Index API client (lives in `:core:network`).

`:core:data` re-exports this module via `api(projects.core.rss)` so existing callers of `cx.aswin.boxlore.core.data.RssPodcastRepository` continue to compile without adding a direct `:core:rss` dependency.

## Public API

- **`RssFeedClient`** — HTTP fetch, conditional HEAD checks (ETag / Last-Modified), dual-parser (rss-parser library + custom XmlPullParser). Returns `RssFetchResult` / `ParsedRssFeed` / `RssFreshnessResult`.
- **`RssPodcastRepository`** — implements `RssSubscriptionPort`; add/confirm/refresh subscriptions; episode catalog CRUD; background freshness checks. Singleton: call only via `AppContainer`; never construct ad hoc.
- **`RssIdGenerator`** — deterministic, FQCN-stable IDs:
  - `rss:` namespace for podcast IDs (`rss:<sha256-hex>`)
  - Negative `Long` IDs for episodes (collision-free with positive Podcast Index IDs)
  - See [ID stability rules](#id-and-fqcn-stability) below
- **`RssSourceMatcher`** — heuristic episode/show matching for Podcast Index migration (`feedIdentityMatches`, `likelySameShow`, `findMatchingEpisode`)
- **`ports.DownloadCacheRelinker`** — `fun interface` injected into `RssPodcastRepository` by `AppContainer` so the RSS module does not compile-depend on `:core:downloads`

## Internal structure

```text
src/main/java/cx/aswin/boxlore/core/data/
  RssFeedClient.kt          # HTTP + XML parsing; RssIdGenerator + RssSourceMatcher inside
  RssPodcastRepository.kt   # RssSubscriptionPort impl; escapeForSqlLike helper
  ports/
    DownloadCacheRelinker.kt  # fun interface — implemented in :core:downloads, wired by AppContainer
```

> **Package note:** all sources keep the package `cx.aswin.boxlore.core.data` (not `…core.rss`). This is intentional — FQCN stability is required for WorkManager worker registrations and for cross-module opaque references to episode/podcast IDs that are already persisted in Room.

## Dependencies

- → `:core:database` (`api` — re-exports `BoxLoreDatabase`, `PodcastEntity`, `RssEpisodeEntity`, …)
- → `:core:domain` (`api` — re-exports `RssSubscriptionPort`, `RssSubscriptionResult`)
- → `:core:model` (internal — `Episode`, `Podcast`, `Person`, `Transcript`)
- → `com.prof18.rssparser` — Atom/RSS library parser path
- → `com.squareup.okhttp3:okhttp` — HTTP fetch + HEAD validators
- → Firebase Cloud Messaging — topic unsubscription on Podcast Index link migration

Forbidden edges: `core:rss` **must not** depend on `:core:downloads` (cycle), `:core:playback`, `:core:designsystem`, or any `:feature:*` module.

## ID and FQCN stability

These identifiers are persisted in Room and must never change:

| Stable value | Why |
| :--- | :--- |
| Podcast IDs: `rss:<sha256-hex>` | Room `podcasts` table PK; user subscriptions |
| Episode IDs: negative `Long` string | Room `rss_episodes` PK; listening history FK; download FK |
| Package `cx.aswin.boxlore.core.data` | Opaque references in WorkManager requests |

**Negative episode IDs:** `RssIdGenerator.episodeIdForPodcast` maps the first 8 bytes of `SHA-256(podcastId + "\0" + identity)` to a positive `Long`, then negates it. This guarantees:
- IDs are always `< 0` (never collide with positive Podcast Index IDs)
- IDs are never `0` or `Long.MIN_VALUE`
- The same input always produces the same ID (deterministic across app restarts)

**`rss:` namespace:** Podcast IDs start with `rss:` followed by a 64-hex-digit SHA-256 of the normalised, fragment-stripped feed URL. Feed URL normalisation (lower-case host, strip fragment) is applied before hashing so minor URL variations map to the same podcast.

## DownloadCacheRelinker port pattern

`RssPodcastRepository` relies on `DownloadCacheRelinker` (a `fun interface` in `ports/`) to re-key Media3 download cache entries when an RSS episode ID changes during Podcast Index migration. The implementation (`DownloadRepository.relinkDownloadCache`) lives in `:core:downloads`. `AppContainer` wires the real lambda after constructing both repositories:

```kotlin
rssPodcastRepository.setDownloadCacheRelinker(
    DownloadCacheRelinker { oldId, newId ->
        DownloadRepository.relinkDownloadCache(appContext, oldId, newId)
    }
)
```

A no-op default is used before wiring, so the repo is safe to construct before `AppContainer` completes.

## Threading / lifecycle

- `RssPodcastRepository` is Application-scoped (singleton via `getInstance`). Only `AppContainer` calls `getInstance`; workers and services obtain the instance from `SharedAppDependenciesHolder.require().rssPodcastRepository`.
- All suspend functions dispatch to `Dispatchers.IO`. Background freshness sweeps (`checkSubscribedFeedFreshness`) use a bounded `Semaphore(4)` to cap concurrent HEAD requests.
- `refreshLocks` — per-podcast `Mutex` prevents duplicate concurrent catalog refreshes for the same feed.

## Testing notes

JVM unit tests under `src/test` cover deterministic ID contracts and episode matching heuristics:

- `RssIdGeneratorTest` — `rss:` prefix, HTTPS enforcement, deterministic negative episode IDs, collision-freedom with Podcast Index IDs, cross-feed independence
- `RssSourceMatcherTest` — URL canonical matching, GUID identity, author conflict rejection, enclosure URL priority, ambiguous title handling

```bash
./gradlew :core:rss:testDebugUnitTest
```

## CI relevance

Included in the unit-test CI job (`testDebugUnitTest`). Kover `merged` variant is added to the project-level coverage report.

## See also

- Root [`ARCHITECTURE.md`](../../ARCHITECTURE.md)
- [`:core:data` README](../data/README.md) — catalog/orchestration layer that re-exports this module
- [`:core:downloads` README](../downloads/README.md) — `DownloadCacheRelinker` implementation lives here
- [`:core:domain` README](../domain/README.md) — `RssSubscriptionPort` definition
- [`docs/PLAN_MODULAR_ANDROID_HARDENING.md`](../../docs/PLAN_MODULAR_ANDROID_HARDENING.md) (Phase A6a)

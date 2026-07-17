# `:app`

## Purpose

Application shell: `BoxLoreApplication`, `MainActivity` NavHost, FCM, surveys, and the composition root (`AppContainer`). Does not own feature UI or data logic beyond wiring.

## Public API

- `BoxLoreApplication.container` — shared DB/repos/managers
- `AppContainer` — factory for the shared graph (order: DB → PodcastRepository → QueueRepository → PlaybackRepository → QueueManager → SmartDownloadManager)
- `LegacyWorkerFactory` — maps pre-rename WorkManager worker FQCNs for one release
- `MainActivity` — nav graph, player overlay (`PlayerSheetScaffold`), deep links

## Internal structure

```text
src/main/java/cx/aswin/boxlore/
  AppContainer.kt
  BoxLoreApplication.kt
  MainActivity.kt
  LegacyWorkerFactory.kt
  fcm/ surveys/ ui/
```

`applicationId` is `cx.aswin.boxlore` (do not change with package renames).

## Dependencies

- → all `:feature:*`, `:core:data`, `:core:designsystem`, `:core:model`, `:core:network`
- Firebase, PostHog, WorkManager, Media3 session client usage via data layer

## Testing notes

- JVM: `src/test` (e.g. FCM payload parser)
- Compose/androidTest and Maestro arrive in later phases

## See also

- Root [`ARCHITECTURE.md`](../ARCHITECTURE.md)

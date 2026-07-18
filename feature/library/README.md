# `:feature:library`

## Purpose

Library hub and sub-routes: subscriptions, liked, downloads, history, show details, smart/auto-download settings.

## Public API

- `LibraryScreen` / `LibraryViewModel` (constructor-injected repos + scorer)
- `HistoryScreen` / `HistoryViewModel`
- `SubscriptionsScreen`, `LikedEpisodesScreen`, `DownloadedEpisodesScreen`
- Download settings screens

## Internal structure

```text
src/main/java/cx/aswin/boxlore/feature/library/
```

## Dependencies

- → `:core:model`, `:core:data`, `:core:designsystem`

## Testing notes

- Prefer fakes for Library/History in P16+
- Routes must use container instances (no local repo recreation)

## See also

- Root [`ARCHITECTURE.md`](../../ARCHITECTURE.md)

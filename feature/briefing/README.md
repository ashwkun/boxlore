# `:feature:briefing`

## Purpose

Daily briefing playback UI and ViewModel (region selection, chapters/transcript wiring via podcast repository).

## Public API

- `BriefingScreen` / `BriefingViewModel` (injected `PodcastRepository`, `PlaybackRepository`, `QueueManager`)

## Internal structure

```text
src/main/java/cx/aswin/boxlore/feature/briefing/
```

## Dependencies

- → `:core:model`, `:core:data`, `:core:designsystem`

## Testing notes

- Smoke: open briefing from home; play/resume via shared playback graph

## See also

- Root [`ARCHITECTURE.md`](../../ARCHITECTURE.md)

# `:feature:player`

## Purpose

Player overlay UI (mini + full sheet) and pure layout/control logic. Playback engine and `PlaybackRepository` live in `:core:data` (later `:core:playback`).

## Public API

- `v2.PlayerSheetScaffold` — mini/full player overlay (not a NavHost route)
- `QueueScreen`, control/seek/transcript UI pieces
- `v2.logic.*` — JVM-testable layout/control helpers (do not rewrite behavior casually)

## Internal structure

```text
src/main/java/cx/aswin/boxlore/feature/player/
  v2/          # sheet, mini/full, theme
  v2/logic/    # pure logic for unit tests
```

## Dependencies

- → `:core:model`, `:core:data`, `:core:network`, `:core:designsystem`
- Media3 UI/session client pieces as needed for controls

## Testing notes

- Strong JVM coverage under `src/test/.../v2/logic` — migrate to JUnit 5 later; **do not rewrite** behavior
- Compose UI tags for play/pause/queue/mini-player in P24

## See also

- Root [`ARCHITECTURE.md`](../../ARCHITECTURE.md)

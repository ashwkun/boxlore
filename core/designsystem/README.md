# `:core:designsystem`

## Purpose

Shared Compose theme, shapes, loaders, image helpers, and share-card UI (`ShareManager`). No feature navigation or repositories.

## Public API

- Theme: `ExpressiveShapes`, motion/typography helpers, dynamic color helpers
- Components: `OptimizedImage`, `BoxLoreLoader`, `AdvancedPlayerControls`, navigation-height constants
- `share.ShareManager` — composite share cards / system share sheet

## Internal structure

```text
src/main/java/cx/aswin/boxlore/core/designsystem/
  components/ theme/ share/ component/
src/main/res/ drawable/ font/
```

## Dependencies

- → `:core:model`
- Compose Material3, Coil (api), coroutines

## Testing notes

- Prefer screenshot / Compose UI tests at app or feature level later; keep this module free of business logic tests

## See also

- Root [`ARCHITECTURE.md`](../../ARCHITECTURE.md)

# `:core:model`

## Purpose

Pure domain models and enums shared across network, data, and UI. No Android framework dependencies beyond what the module already declares for serialization.

## Public API

- Podcast / Episode / Briefing / Chapter-related models
- `PlaybackEntryPoint`, `ShareTarget`, `ShareLinkBuilder`
- `AutoTranscriptState` (playback + player UI)
- Cross-promotion model types

## Internal structure

```text
src/main/java/cx/aswin/boxlore/core/model/
```

## Dependencies

- Kotlinx Serialization only (no project deps)

## Testing notes

- Prefer pure JVM unit tests for formatters/builders when added

## See also

- Root [`ARCHITECTURE.md`](../../ARCHITECTURE.md)

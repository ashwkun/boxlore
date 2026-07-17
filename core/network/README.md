# `:core:network`

## Purpose

HTTP / Retrofit API surface and DTOs for the Boxlore backend (Podcast Index proxy, content sections, etc.). No Room or Compose.

## Public API

- `NetworkModule` / `BoxLoreApi`
- Network model types under `core.network.model`
- App Check / version header hooks used by the app

## Internal structure

```text
src/main/java/cx/aswin/boxlore/core/network/
  model/
```

## Dependencies

- → `:core:model`
- OkHttp, Retrofit, Gson/serialization as configured in Gradle

## Testing notes

- JVM tests for request serialization (e.g. content sections)

## See also

- Root [`ARCHITECTURE.md`](../../ARCHITECTURE.md)

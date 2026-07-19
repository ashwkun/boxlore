# Screenshot baselines

This document describes the current screenshot and visual-regression reference path. Screenshot comparison is not a required CI gate at this time.

## Current coverage

- No PNG goldens are committed under `screenshots/baselines/`.
- `:feature:home` contains `AddRssFeedDialogRoborazziSpikeTest`, which can capture a local Roborazzi image under `feature/home/build/outputs/roborazzi/`.
- CI does not run image diff comparisons.
- Home instrumented tests verify dialog composition through stable Compose test tags.

## Layout

```text
screenshots/baselines/     # committed PNG goldens when visual baselines are adopted
docs/screenshots/          # this reference document
```

Suggested names for future goldens:

```text
screenshots/baselines/add_rss_feed_dialog.png
screenshots/baselines/home_settings_entry.png
```

## Capture

Run the local Roborazzi capture test from the repository root:

```bash
./gradlew :feature:home:testDebugUnitTest --tests '*AddRssFeedDialogRoborazziSpikeTest'
```

The generated images are local build artifacts. For device captures, use a fixed-density emulator, capture the intended screen manually, and commit PNGs under `screenshots/baselines/` only when they are approved baselines.

## Compare

Until a comparison task is wired into CI, review image changes locally with an image diff viewer or through normal pull-request artifact review. Prefer tagged-node assertions for dialogs because Compose `AlertDialog` can produce multiple roots.

## Gradle

No screenshot comparison task is part of required CI. Roborazzi is currently configured for local Home module capture tests.

## See also

- [`docs/TESTING.md`](../TESTING.md)
- [`feature/home/README.md`](../../feature/home/README.md)
- `feature/home/src/androidTest/java/cx/aswin/boxlore/feature/home/settings/`

# Maestro smoke flows

Maestro flows provide device or emulator smoke coverage for the installed Boxlore app. Local runs require a connected device or emulator. CI validates flow files and can run Maestro Cloud when the optional secrets are configured.

## App id

`cx.aswin.boxlore`

## Prerequisites

1. Install [Maestro](https://maestro.mobile.dev/):
   ```bash
   curl -Ls "https://get.maestro.mobile.dev" | bash
   export PATH="$PATH:$HOME/.maestro/bin"
   ```
2. Prepare local CI-style configuration:
   ```bash
   ./scripts/ci/write-cloud-agent-local-config.sh
   export ANDROID_HOME="$HOME/Android/Sdk"
   export ANDROID_SDK_ROOT="$ANDROID_HOME"
   ```
3. Start an emulator or connect a device, then install a debug build:
   ```bash
   ./gradlew :app:installDebug
   ```
4. Complete or skip onboarding on the device when a flow needs the home shell. The launch flow treats consent and onboarding taps as optional but requires the Home settings button after launch.

## Run

From the repository root:

```bash
maestro test maestro/
```

Run a single flow:

```bash
maestro test maestro/smoke_launch.yaml
maestro test maestro/smoke_settings_rss.yaml
maestro test maestro/smoke_home_visible.yaml
```

## Flows

| File | Intent |
| :--- | :--- |
| `smoke_launch.yaml` | Cold launch with a strict assertion on `home_settings_button` |
| `smoke_home_visible.yaml` | Home visibility with a strict assertion on `home_settings_button` and soft navigation text checks |
| `smoke_settings_rss.yaml` | Settings to Library to Add RSS, with optional steps for device state variance |

Flows prefer Compose `testTag`s such as `home_settings_button` and `settings_add_rss_*`, with visible text fallback where noted. Optional first-run taps keep flows usable across devices with different onboarding or consent state.

## CI

Workflow: [`.github/workflows/maestro-nightly.yml`](../.github/workflows/maestro-nightly.yml)

| Trigger | Behavior |
| :--- | :--- |
| Cron or `workflow_dispatch` | Validates `maestro/*.yaml` presence and basic syntax |
| Optional device-farm job | Builds `:app:assembleDebug` and runs Maestro Cloud when secrets are set |

Optional secrets for full device-farm runs:

- `MAESTRO_CLOUD_API_KEY`
- `MAESTRO_PROJECT_ID`

Without those secrets, the workflow still validates flow files and reports that the device-farm job is skipped.

## Notes

- Complete or skip onboarding once on a local test device for more stable Settings navigation.
- Do not commit secrets or production `google-services.json` for Maestro runs.
- See [`docs/TESTING.md`](../docs/TESTING.md) for the broader test strategy.

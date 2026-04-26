# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run a single unit test class
./gradlew test --tests "uri.app.kickflip.ExampleUnitTest"

# Clean build
./gradlew clean assembleDebug
```

## Architecture Overview

KickFlip is an Android skateboarding app (Java, minSdk 28, targetSdk 34) with two primary activities and a fragment-based navigation system.

### App Flow

**Entry point:** `MainActivity` (launcher) — shows the landing screen with Register/Sign In buttons. Authentication is handled through `RegisterDialog` and `LoginDialog` (both `DialogFragment`s using Firebase Auth). On successful login, the user is navigated to `HomeActivity`.

**Main app shell:** `HomeActivity` — hosts a `BottomNavigationView` with a single `FrameLayout` container (`R.id.homeFragmentContainer`). Fragments are swapped into this container based on the selected nav item.

### Current Navigation Tabs

| Nav Item | Fragment | Status |
|---|---|---|
| `nav_weather` | `WeatherFragment` | Implemented |
| `nav_vault` | `VaultFragment` | Implemented |
| `nav_progression` | ProgressionFragment | TODO (placeholder) |
| `nav_profile` | ProfileFragment | TODO (placeholder) |

### Key Fragments

**`WeatherFragment`** — Lets users search skate spots by location name. Uses `weatherApiService` (plain `HttpURLConnection` on an `ExecutorService`, results posted back to main thread via `Handler`) to hit OpenWeatherMap's `/weather` and `/forecast` endpoints. Creates `SkateSpot` objects whose `calculateGroundStatus()` determines whether a spot is dry/drying/wet based on rain recency, temperature, and humidity.

**`VaultFragment`** — Displays logged tricks in a 2-column `GridLayoutManager` RecyclerView via `TrickAdapter`. The FAB navigates to `AddTrickFragment` (pushed onto the back stack so the back button returns to the Vault). Tap a trick card → detail dialog; long-press → options (TODO). **Firebase fetch is not yet implemented** — currently loads dummy data.

**`AddTrickFragment`** — Form for logging a new trick. Uses `GridLayout` (not `RecyclerView`) to render trick and terrain options as selectable tiles. Difficulty is calculated from a hardcoded `trickDifficulty[]` array + terrain modifier. Supports optional video selection from device gallery. **Database save is not yet implemented** (the `saveTrickToDatabase()` method is a stub).

### Data Model

`TrickEntry` is defined as a static inner class in both `VaultFragment` and `AddTrickFragment` — these are duplicates and should eventually be unified into a shared model class. The `VaultFragment.TrickEntry` includes a no-arg constructor for Firebase compatibility.

### External Dependencies

- **Firebase Auth** (`firebase-bom:34.6.0`) — email/password auth only
- **OpenWeatherMap API** — API key is hardcoded in `weatherApiService.java` (`API_KEY` field); uses `/data/2.5/weather` and `/data/2.5/forecast`
- **RecyclerView**, **CardView**, **Material Components**, **ConstraintLayout**

### Pending Work (TODOs in code)

- Firebase Firestore/Realtime Database integration for persisting tricks (`VaultFragment.loadTricksFromDatabase`, `AddTrickFragment.saveTrickToDatabase`)
- Long-press trick options (edit/delete bottom sheet) in `VaultFragment.showTrickOptions`
- `ProgressionFragment` and `ProfileFragment` are unimplemented stubs in `HomeActivity`

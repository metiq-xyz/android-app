# Contributing to Metiq

Thanks for thinking about helping out! This document covers everything you need
to know to get a local copy of Metiq running, and to find your way around the
codebase before you start hacking on it.

If anything in here is unclear, please open an issue — that's almost always a
sign that the documentation needs improving.

## Reporting bugs and suggesting features

The simplest contribution is a well-written issue. Before you open one:

- **Search the existing issues** to see if it's already been reported.
- **Include your Android version and device model** when reporting a bug.
- **Tell us what you expected and what you saw instead.** A short video or
  screen recording is gold for UI bugs.

For feature suggestions, please describe the problem you're trying to solve as
well as the solution you have in mind. We're more likely to act on "I have
trouble doing X because Y" than on "please add feature Z".

## Translating

Metiq currently ships in English, Italian, Spanish, French, and Portuguese.

If you spot a translation that sounds off, or you'd like to add a new language,
the strings live in `app/src/main/res/values-<lang>/strings.xml`. Send a pull
request and we'll review.

## Setting up your development environment

### Prerequisites

- **Android Studio** — Ladybug (2024.2) or newer
- **JDK 17** — bundled with Android Studio; if you build from the command line,
  make sure `JAVA_HOME` points at a JDK 17 installation
- **A device or emulator** running Android 9 (API 28) or newer

### Cloning and opening

```bash
git clone https://github.com/metiq-xyz/android-app.git metiq-android app
cd metiq-android-app
```

Open the cloned directory in Android Studio. Studio will offer to install the
Gradle wrapper and sync the project — accept both. The first sync downloads the
Compose, Material 3, AndroidX, and Media3 dependencies, which can take a couple
of minutes.

### A note on audio assets

The four colored-noise loops (pink, brown, white, grey) and the ambient sounds
the app plays are committed directly to this repository under
`app/src/main/assets/audio/`.

The colored-noise OGGs are produced by a sibling project,
[`metiq-xyz/colored-noise-generator`](https://github.com/metiq-xyz/colored-noise-generator),
which holds the Node.js + ffmpeg pipeline that synthesizes, loudness-normalizes
and encodes them. That repo is only run when the noise needs to change; the
encoded artifacts are then copied into this repo and committed.

### Building and running

From Android Studio, pick a build variant from the **Build Variants** tool
window:

- `fdroidDebug` — default; matches the F-Droid distribution
- `playDebug` — matches the Google Play distribution

Both flavors share `applicationId = xyz.metiq`, so they can't be installed side
by side. The flavor only changes the "Rate Metiq" link target inside the app's
settings.

From the command line:

```bash
./gradlew assembleFdroidDebug      # F-Droid debug APK
./gradlew assemblePlayDebug        # Play debug APK
./gradlew assembleFdroidRelease    # F-Droid release APK (signed if a keystore is configured)
./gradlew assemblePlayRelease      # Play release APK
```

The output APKs end up under `app/build/outputs/apk/<flavor>/<buildType>/`.

### Optional: configure release signing

Release builds are signed with your debug key by default, which is fine for
installing on your own phone. To sign with a proper release key — required for
publishing to F-Droid or the Play Store — create a `keystore.properties` file at
the project root (it's gitignored):

```properties
storeFile=/absolute/path/to/your/release-keystore.jks
storePassword=your-store-password
keyAlias=your-key-alias
keyPassword=your-key-password
```

If you haven't generated a keystore yet:

```bash
keytool -genkey -v -keystore ~/.android/metiq-release.jks \
  -alias metiq -keyalg RSA -keysize 2048 -validity 10000
```

Back up that keystore file somewhere safe. If you lose it, you can never publish
a signed update for the same package name again.

## Repository layout

```
metiq/
├── app/                      # Android app module (Kotlin + Jetpack Compose)
│   ├── src/main/java/xyz/metiq/
│   │   ├── audio/            # AudioEngine, EnginePlayer, PlaybackService, AudioRouteObserver
│   │   ├── ui/               # Compose UI: HomeScreen, SettingsScreen, LicensesScreen, components, theme
│   │   ├── MainActivity.kt   # Single Activity hosting the Compose nav
│   │   ├── MetiqApp.kt       # Application subclass; wires the settings store
│   │   └── Settings.kt       # DataStore-backed settings repository
│   ├── src/main/res/         # Layouts (none), drawables, strings, themes, fonts
│   └── build.gradle.kts
├── gradle/                   # Gradle wrapper and version catalog (libs.versions.toml)
├── .github/workflows/        # CI: builds APKs on tag push, attaches them to a draft release
├── build.gradle.kts          # Root Gradle build
└── settings.gradle.kts
```

## Code style and a few conventions

- **No comments unless the _why_ is genuinely non-obvious.** The codebase aims
  to be self-explanatory through naming. If you find yourself writing a comment,
  ask whether a clearer name or a small refactor would make it unnecessary.
- **Strings live in `strings.xml`.** Anything the user can read goes there,
  including content descriptions for icons.
- **Avoid Compose anti-patterns.** Don't allocate lambdas inside loops, prefer
  `remember(...)` for derived state, and keep the composition shallow.
- **Match the existing visual rhythm.** New settings entries should use the
  existing `ToggleRow`, `LinkRow`, or `DropdownPickerRow` so the section padding
  and ripple behaviour stays consistent.

## Pull request workflow

1. Fork the repository and create a topic branch off `main`.
2. Make your change. Keep commits small and self-contained where possible —
   large mixed-purpose commits are hard to review and harder to revert.
3. If you touched anything user-facing, double-check the relevant `strings.xml`
   files. English is the source of truth.
4. Run a debug build locally to make sure everything still compiles. The CI
   workflow also builds both flavors on every push, but it's nicer to catch
   problems early.
5. Open the pull request against `main` and describe the _why_ alongside the
   _what_.

We try to review pull requests within a week. If yours has gone quiet for
longer, a friendly ping on the PR is always welcome.

## Continuous integration

Every push to `main` and every pull request runs both flavor builds. On a `v*`
tag push, the workflow additionally signs the release APKs (if the appropriate
repository secrets are configured), uploads them as build artifacts, and creates
a draft GitHub Release with both APKs attached.

The CI definition lives at `.github/workflows/build-apks.yml`.

## Questions?

Open an issue — we'd rather answer the same question publicly twice than have
someone struggle in private.

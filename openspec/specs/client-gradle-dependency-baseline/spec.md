## Purpose
Record the client Gradle dependency baseline, Android SDK requirement, and warning cleanup expectations for the Compose Multiplatform app.

## Requirements

### Requirement: Client Gradle dependency baseline
The client project SHALL use a verified Compose Multiplatform and Android dependency baseline compatible with the installed Android SDK.

#### Scenario: Client dependency baseline is applied
- **WHEN** the client project is built after dependency updates
- **THEN** Gradle wrapper SHALL use Gradle 9.4.1
- **AND** Android Gradle Plugin SHALL use 9.2.1
- **AND** Kotlin SHALL use 2.3.21
- **AND** `android-compileSdk` SHALL be 37
- **AND** Compose Multiplatform SHALL use 1.11.0
- **AND** Compose Material3 Adaptive SHALL use 1.3.0-beta01
- **AND** Android debug build SHALL pass with `./gradlew :composeApp:assembleDebug`.

### Requirement: Client build warnings are minimized
The client project SHALL use non-deprecated source APIs where practical without changing the module structure.

#### Scenario: Compose and Navigation APIs are compiled
- **WHEN** the app navigation shell is compiled
- **THEN** adaptive window info SHALL use `currentWindowAdaptiveInfoV2`
- **AND** Navigation3 scenes SHALL use the `sceneStrategies` list API
- **AND** Compose previews SHALL import `androidx.compose.ui.tooling.preview.Preview`.

#### Scenario: AGP 9 KMP compatibility remains transitional
- **WHEN** AGP 9 is used with the current Kotlin Multiplatform Android application module
- **THEN** temporary compatibility properties MAY remain enabled until the Android application is split into a dedicated subproject.
- **AND** a future structural migration SHALL remove the AGP 9 compatibility warnings.

### Requirement: Client dependency exceptions are explicit
The client project SHALL keep dependencies at the newest version that resolves and builds for all required modules.

#### Scenario: Ktorfit converter artifacts are unavailable
- **WHEN** Ktorfit converter artifacts are not published for the newest Ktorfit library version
- **THEN** the client SHALL use the newest Ktorfit version whose required artifacts resolve
- **AND** unused Ktorfit converter dependencies SHALL be omitted when a custom converter is used.

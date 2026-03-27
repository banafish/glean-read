# Specification: Network Configuration

## Capabilities

The Android application should be able to:
- Manage API base URLs centrally through the build system (`build.gradle.kts`).
- Define all network endpoints in a single, reusable Kotlin object (`ApiConstants`).
- Support switching between different backend environments without changing Java/Kotlin source code.

## New Requirements

### Requirement: Centralized URL Base
- The base URL must be defined as a `buildConfigField` in the `app` module's `build.gradle.kts`.
- The field name should be `BASE_URL`.
- The default value for development should be `http://10.0.2.2:8080`.

### Requirement: API Endpoint Constant
- A new file `ApiConstants.kt` must be created in `com.gleanread.android.network`.
- The `ApiConstants` object should derive full URLs by combining the base URL from `BuildConfig` with specific path strings.
- Example endpoint: `val CAPTURE = "${BuildConfig.BASE_URL}/api/v1/glean/capture"` (or similar).

### Requirement: Usage in Activities
- Classes currently using hardcoded URLs (like `FastCaptureActivity.kt`) must be refactored to use the relevant constant from `ApiConstants`.

## Removed Requirements
- Hardcoded string URLs in source code files.

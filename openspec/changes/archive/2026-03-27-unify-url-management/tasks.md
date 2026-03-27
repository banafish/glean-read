# Implementation Tasks: Unify URL Management

1. Build Configuration Setup

- [x] 1.1 Enable `buildConfig` in `app/build.gradle.kts`
- [x] 1.2 Define `BASE_URL` in `app/build.gradle.kts` using `buildConfigField`

2. Centralized Constant Management

- [x] 2.1 Create `com.gleanread.android.network.ApiConstants` object
- [x] 2.2 Import `BuildConfig` and define URL-based constants

3. Activity Refactoring

- [x] 3.1 Replace hardcoded URL in `FastCaptureActivity.kt` with `ApiConstants` reference
- [x] 3.2 Verify the application can still successfully post to the API (log-based check)

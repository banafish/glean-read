# Design: Unify URL Management in GleanRead Android

## Context

API URLs currently reside as string literals directly in Activity classes like `FastCaptureActivity.kt`. This creates technical debt and complicates environment-specific deployments.

## Goals / Non-Goals

**Goals:**
- Externalize the base API URL to build configuration.
- Centralize endpoint paths in a dedicated constant object.
- Maintain compatibility with the existing HTTP network calls.

**Non-Goals:**
- Introduce Retrofit or OkHttp (unless already in use, which it isn't based on the imports).
- Change the underlying POST request structure.

## Design Decisions

1. **Gradle Build Configuration**:
   - Add `buildConfigField` to the `defaultConfig` in `app/build.gradle.kts`.
   - The field name will be `BASE_URL`.
   - The default value will be `"http://10.0.2.2:8080"` (Android emulator localhost).
   - Enable `buildConfig` feature in `buildFeatures`.

2. **Centralized Constants File**:
   - Create `com.gleanread.android.network.ApiConstants`.
   - Define endpoints relative to the base URL provided by `BuildConfig`.

3. **Code Modification**:
   - Update `FastCaptureActivity.kt` to use `ApiConstants.CAPTURE_ENDPOINT` or similar.

## Implementation Details

### Gradle Configuration

```kotlin
android {
    ...
    buildFeatures {
        compose = true
        buildConfig = true
    }
    defaultConfig {
        ...
        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080\"")
    }
}
```

### ApiConstants

```kotlin
package com.gleanread.android.network

import com.gleanread.android.BuildConfig

object ApiConstants {
    private const val BASE_URL = BuildConfig.BASE_URL
    const val CAPTURE_ENDPOINT = "$BASE_URL/api/v1/glean/capture"
}
```

## Risks / Trade-offs

- **Risk**: `BuildConfig` generation requires a project sync/rebuild.
- **Trade-offs**: Slightly more abstraction than string literals, but worth it for maintainability.

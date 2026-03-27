# 提案: 统一管理 GleanRead Android 的 URL

## 为什么

目前 `glean-read-android` 项目中的 API URL 是硬编码的（例如在 `FastCaptureActivity.kt` 中）。这导致以下问题：
- 不修改源码无法更改后端服务器地址。
- 难以管理不同的环境（如开发、测试、生产）。
- 无法维护一个清晰且一致的代码库，网络依赖关系未集中管理。

使用常见的 Android 配置模式（如 `BuildConfig` 和 `ApiConstants` 对象）统一管理 URL，将提高代码的可维护性和灵活性。

## 变更内容

1. **构建配置**：将 API 基础 URL 移动到 `build.gradle.kts`，使用 `buildConfigField` 管理。
2. **集中常量管理**：创建 `ApiConstants` Kotlin 对象来存储基础 URL 和端点路径。
3. **配置访问**：在代码中通过 `BuildConfig` 访问 URL，替换硬编码的字符串。

## Impact

- **glean-read-android/app/build.gradle.kts**: Will be updated to include `buildConfigField`.
- **FastCaptureActivity.kt**: Hardcoded URLs will be replaced with constants.
- **New File**: `ApiConstants.kt` will be created to hold endpoint-specific information.
- No changes to functional logic, only how URLs are stored and accessed.

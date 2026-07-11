package com.gleanread.android.platform.page_context.wechat

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * 服务进程 overlay 的 Compose 宿主生命周期。
 * ComposeView 依赖 ViewTree 上的 LifecycleOwner / SavedStateRegistryOwner，
 * 无障碍服务没有现成宿主，这里手工提供一个最小实现：
 * 每次显示气泡新建、隐藏即销毁，不复用 Recomposer，规避服务长生命周期下的 Compose 悬挂。
 */
internal class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    /** 必须在 WindowManager.addView 之前挂到 view tree 上 */
    fun attachTo(view: View) {
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
    }

    /** addView 成功后调用，驱动 Compose 开始渲染 */
    fun moveToResumed() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    /** removeView 之前调用，确保 Composition 正常释放 */
    fun destroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}

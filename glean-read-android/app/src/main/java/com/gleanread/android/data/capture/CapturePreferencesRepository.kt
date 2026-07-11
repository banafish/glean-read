package com.gleanread.android.data.capture

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.captureDataStore: DataStore<Preferences> by preferencesDataStore(name = "capture_preferences")

/**
 * 摘录能力偏好仓库。
 * 当前仅承载「微信摘录气泡」开关；开关独立于系统无障碍服务状态，默认开启。
 */
class CapturePreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.applicationContext.captureDataStore)

    private val wechatBubbleEnabledKey = booleanPreferencesKey("wechat_capture_bubble_enabled")

    val isWeChatBubbleEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[wechatBubbleEnabledKey] ?: true
    }

    suspend fun setWeChatBubbleEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[wechatBubbleEnabledKey] = enabled
        }
    }
}

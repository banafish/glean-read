package com.gleanread.android.data.capture

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class CapturePreferencesRepositoryTest {
    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    @Test
    fun `wechat bubble defaults to enabled`() = runTest {
        val repository = createRepository("default")

        assertTrue(repository.isWeChatBubbleEnabledFlow.first())
    }

    @Test
    fun `wechat bubble can be disabled and read back`() = runTest {
        val repository = createRepository("disable")

        repository.setWeChatBubbleEnabled(false)

        assertFalse(repository.isWeChatBubbleEnabledFlow.first())
    }

    @Test
    fun `wechat bubble can be re-enabled`() = runTest {
        val repository = createRepository("re_enable")

        repository.setWeChatBubbleEnabled(false)
        repository.setWeChatBubbleEnabled(true)

        assertTrue(repository.isWeChatBubbleEnabledFlow.first())
    }

    /** 每个用例独立的临时 DataStore 文件，避免单例 DataStore 跨用例串状态 */
    private fun TestScope.createRepository(name: String): CapturePreferencesRepository {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tmpFolder.root, "capture_$name.preferences_pb") },
        )
        return CapturePreferencesRepository(dataStore)
    }
}

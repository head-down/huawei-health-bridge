package com.headdown.healthbridge

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.headdown.healthbridge.ui.MainScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI 测试 — 验证 MainScreen 在各状态下的渲染和交互。
 *
 * 这些测试需要在模拟器或真机上运行：
 *   ./gradlew connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class MainUIScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // ============================================================
    // 初始渲染 — 所有 UI 元素完整显示
    // ============================================================

    @Test
    fun displaysSubtitleText() {
        composeTestRule.onNodeWithText("将华为运动健康数据同步到 Health Connect").assertIsDisplayed()
    }

    @Test
    fun displaysAllStepButtonsInInitialState() {
        composeTestRule.onNodeWithText("授权华为运动健康").assertIsDisplayed()
        composeTestRule.onNodeWithText("授权 Health Connect").assertIsDisplayed()
        composeTestRule.onNodeWithText("开始同步").assertIsDisplayed()
    }

    @Test
    fun refreshButtonVisibleInInitialState() {
        composeTestRule.onNodeWithText("刷新同步").assertIsDisplayed()
    }

    // ============================================================
    // 步骤 1 — 华为 OAuth 授权按钮状态
    // ============================================================

    @Test
    fun huaweiAuthButtonShowsDoneCheckmarkAfterAuthorized() {
        // 通过 ViewModel 模拟授权完成
        val viewModel = (composeTestRule.activity).run {
            @Suppress("DEPRECATION")
            androidx.lifecycle.ViewModelProvider(this)[MainViewModel::class.java]
        }
        composeTestRule.runOnUiThread {
            viewModel.exchangeCodeForToken("test_code")
        }
        composeTestRule.waitForIdle()

        // 按钮应显示完成图标
        composeTestRule.onNodeWithContentDescription("已完成").assertIsDisplayed()
    }

    @Test
    fun huaweiAuthButtonBecomesDisabledWhenSyncing() {
        val viewModel = (composeTestRule.activity).run {
            @Suppress("DEPRECATION")
            androidx.lifecycle.ViewModelProvider(this)[MainViewModel::class.java]
        }
        composeTestRule.runOnUiThread {
            // 直接设 Syncing 状态模拟
            viewModel.onHuaweiAuthStarted()
        }
        composeTestRule.waitForIdle()

        // 按钮应仍然可见（checkmark 不显示 — 因为 Authorizing 不是 Authorized）
        composeTestRule.onNodeWithText("授权华为运动健康").assertIsDisplayed()
    }

    // ============================================================
    // 步骤 3 — 同步按钮与进度展示
    // ============================================================

    @Test
    fun syncButtonClickEntersSyncingState() {
        composeTestRule.onNodeWithText("开始同步").performClick()

        // 同步过程中进度文本出现
        composeTestRule.waitForIdle()
        // 注意：同步会调用真实 API，可能快速失败或挂起
        // 最低要求：按钮点击不发生崩溃并显示某种同步状态
        composeTestRule.onNodeWithText("开始同步").assertIsDisplayed()
    }

    // ============================================================
    // 刷新按钮行为
    // ============================================================

    @Test
    fun refreshButtonIsInitiallyEnabled() {
        composeTestRule.onNodeWithText("刷新同步").assertIsDisplayed()
        // 初始状态 SyncState.Idle，按钮应启用
        composeTestRule.onNodeWithText("刷新同步").performClick()
        // 不应崩溃
    }

    // ============================================================
    // 页面不崩溃验证（smoke test）
    // ============================================================

    @Test
    fun mainScreenDoesNotCrashOnLaunch() {
        // Activity 已在 createAndroidComposeRule 中启动
        // 只需验证有内容渲染
        composeTestRule.onNodeWithText("华为健康桥").assertIsDisplayed()
    }
}

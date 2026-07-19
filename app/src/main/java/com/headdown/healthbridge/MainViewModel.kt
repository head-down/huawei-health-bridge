package com.headdown.healthbridge

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.headdown.healthbridge.data.AuthProvider
import com.headdown.healthbridge.data.DataType
import com.headdown.healthbridge.data.SyncPreferences
import com.headdown.healthbridge.data.SyncResult
import com.headdown.healthbridge.healthconnect.HealthConnectWriter
import com.headdown.healthbridge.huawei.HuaweiHealthClient
import com.headdown.healthbridge.sync.SyncProgress
import com.headdown.healthbridge.sync.Syncer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val huaweiAuthState: HuaweiAuthState = HuaweiAuthState.Idle,
    val healthConnectState: HealthConnectState = HealthConnectState.Idle,
    val syncState: SyncState = SyncState.Idle,
    val syncResult: SyncResult? = null,
    val syncError: String? = null,
    val syncedTypes: List<DataType> = emptyList(),
    val syncProgress: Int = 0,
)

enum class HuaweiAuthState {
    Idle,
    Authorizing,
    Authorized,
}

enum class HealthConnectState {
    Idle,
    Granting,
    Granted,
}

enum class SyncState {
    Idle,
    Syncing,
    Success,
    Error,
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val huaweiClient: HuaweiHealthClient by lazy {
        HuaweiHealthClient(application)
    }

    private val syncPrefs by lazy {
        SyncPreferences(application)
    }

    private val healthConnectClient: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(application)
    }

    private val healthConnectWriter: HealthConnectWriter by lazy {
        HealthConnectWriter(healthConnectClient)
    }

    /**
     * 华为 OAuth 授权 URL。
     * Activity 调用此方法获取 URL 后启动浏览器。
     */
    fun getAuthorizationUrl(): String = huaweiClient.getAuthorizationUrl()

    /**
     * 用回调 code 换取 access token，成功后更新 UI 状态。
     */
    fun exchangeCodeForToken(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = huaweiClient.exchangeCodeForToken(code)
            _uiState.update {
                if (success) {
                    it.copy(huaweiAuthState = HuaweiAuthState.Authorized)
                } else {
                    it.copy(huaweiAuthState = HuaweiAuthState.Idle,
                        syncError = "华为授权失败，请重试")
                }
            }
        }
    }

    /**
     * 用户点击"授权华为"后，标记授权进行中状态。
     * 外部 Activity 通过 getAuthorizationUrl() 获取 URL 并启动浏览器。
     */
    fun onHuaweiAuthStarted() {
        _uiState.update { it.copy(huaweiAuthState = HuaweiAuthState.Authorizing) }
    }

    /**
     * OAuth 回调成功，标记已授权。
     */
    fun onHuaweiAuthComplete() {
        _uiState.update { it.copy(huaweiAuthState = HuaweiAuthState.Authorized) }
    }

    /**
     * Health Connect 权限授权中。
     */
    fun onHealthConnectGranting() {
        _uiState.update { it.copy(healthConnectState = HealthConnectState.Granting) }
    }

    /**
     * Health Connect 权限已授予。
     */
    fun onHealthConnectGranted() {
        _uiState.update { it.copy(healthConnectState = HealthConnectState.Granted) }
    }

    /**
     * Health Connect 权限被拒绝，状态回归 Idle。
     */
    fun onHealthConnectDenied() {
        _uiState.update { it.copy(healthConnectState = HealthConnectState.Idle) }
    }

    /**
     * 执行同步：查询华为健康数据 → 写入 Health Connect。
     * 显示加载中状态，完成后展示结果摘要或错误。
     */
    fun syncData() {
        if (_uiState.value.syncState == SyncState.Syncing) return

        _uiState.update {
            it.copy(
                syncState = SyncState.Syncing,
                syncError = null,
                syncResult = null,
                syncedTypes = emptyList(),
                syncProgress = 0,
            )
        }

        viewModelScope.launch {
            try {
                val progress = SyncProgress { type ->
                    _uiState.update { state ->
                        state.copy(
                            syncedTypes = state.syncedTypes + type,
                            syncProgress = state.syncedTypes.size + 1
                        )
                    }
                }

                val result = withContext(Dispatchers.IO) {
                    val authProvider = object : AuthProvider {
                        override suspend fun getToken() = huaweiClient.getValidToken()
                        override fun isTokenValid() = huaweiClient.isTokenValid()
                        override suspend fun refreshToken() = getToken()
                    }

                    val syncer = Syncer(huaweiClient, authProvider, healthConnectWriter)

                    val endTime = System.currentTimeMillis()
                    val startTime = syncPrefs.computeStartTime(endTime)

                    val syncResult = syncer.performSync(startTime, endTime, progress)

                    syncPrefs.saveSyncTime(endTime)

                    syncResult
                }

                _uiState.update {
                    it.copy(
                        syncState = SyncState.Success,
                        syncResult = result,
                        huaweiAuthState = HuaweiAuthState.Authorized
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        syncState = SyncState.Error,
                        syncError = e.message ?: "同步失败"
                    )
                }
            }
        }
    }

    /**
     * 手动刷新：重置状态后重新同步。
     */
    fun refreshSync() {
        _uiState.update {
            it.copy(
                syncState = SyncState.Idle,
                syncResult = null,
                syncError = null
            )
        }
        syncData()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    internal fun overrideTestState(state: MainUiState) {
        _uiState.value = state
    }
}

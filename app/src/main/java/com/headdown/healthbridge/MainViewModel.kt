package com.headdown.healthbridge

import android.app.Application
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.headdown.healthbridge.data.AuthProvider
import com.headdown.healthbridge.data.SyncResult
import com.headdown.healthbridge.healthconnect.HealthConnectWriter
import com.headdown.healthbridge.huawei.HuaweiHealthClient
import com.headdown.healthbridge.sync.Syncer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class MainUiState(
    val huaweiAuthState: HuaweiAuthState = HuaweiAuthState.Idle,
    val healthConnectState: HealthConnectState = HealthConnectState.Idle,
    val syncState: SyncState = SyncState.Idle,
    val syncResult: SyncResult? = null,
    val syncError: String? = null,
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

    val huaweiClient: HuaweiHealthClient by lazy {
        HuaweiHealthClient(application)
    }

    private val healthConnectClient: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(application)
    }

    private val healthConnectWriter: HealthConnectWriter by lazy {
        HealthConnectWriter(healthConnectClient)
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
     * Health Connect 权限已授予。
     */
    fun onHealthConnectGranted() {
        _uiState.update { it.copy(healthConnectState = HealthConnectState.Granted) }
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
                healthConnectState = HealthConnectState.Granted
            )
        }

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val authProvider = object : AuthProvider {
                        override suspend fun getToken() = huaweiClient.getValidToken()
                        override fun isTokenValid() = huaweiClient.getValidToken() != null
                        override suspend fun refreshToken() =
                            huaweiClient.getValidToken() // TODO: 真实 refresh_token 刷新
                    }

                    val syncer = Syncer(huaweiClient, authProvider, healthConnectWriter)

                    // 计算同步窗口：最近 30 天
                    val prefs = getApplication<Application>()
                        .getSharedPreferences("sync_prefs", android.content.Context.MODE_PRIVATE)
                    val lastSyncedAt = prefs.getLong("last_synced_at", 0L)
                    val endTime = System.currentTimeMillis()
                    val thirtyDaysAgo = endTime - TimeUnit.DAYS.toMillis(30)
                    val startTime = if (lastSyncedAt > 0) lastSyncedAt else thirtyDaysAgo

                    val syncResult = syncer.performSync(startTime, endTime)

                    // 保存检查点
                    prefs.edit().putLong("last_synced_at", endTime).apply()

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
}

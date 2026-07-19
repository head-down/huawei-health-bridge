package com.headdown.healthbridge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.headdown.healthbridge.data.SyncResult
import com.headdown.healthbridge.sync.SyncWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var mainViewModel: MainViewModel

    /** Health Connect 权限请求启动器 — 字段级声明确保在 Activity RESUMED 前注册 */
    private val healthPermissionsLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(HEALTH_PERMISSIONS)) {
            mainViewModel.onHealthConnectGranted()
            mainViewModel.syncData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // 检查 OAuth 回调
        handleOAuthCallback(intent)

        // 启动定时同步任务
        schedulePeriodicSync()

        setContent {
            MaterialTheme {
                MainScreen(
                    viewModel = mainViewModel,
                    onAuthorizeHuawei = ::startHuaweiAuth,
                    onRequestHealthPermissions = ::requestHealthPermissions
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthCallback(intent)
    }

    private fun handleOAuthCallback(intent: Intent) {
        intent.data?.let { uri ->
            if (uri.scheme == "com.headdown.healthbridge" && uri.host == "oauth") {
                val code = uri.getQueryParameter("code")
                if (code != null) {
                    mainViewModel.exchangeCodeForToken(code)
                }
            }
        }
    }

    private fun startHuaweiAuth() {
        mainViewModel.onHuaweiAuthStarted()
        val authUrl = mainViewModel.getAuthorizationUrl()
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
        startActivity(browserIntent)
    }

    private fun requestHealthPermissions() {
        mainViewModel.onHealthConnectGranting()
        healthPermissionsLauncher.launch(HEALTH_PERMISSIONS)
    }

    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<SyncWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "huawei_health_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    companion object {
        private val HEALTH_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getWritePermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getWritePermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getWritePermission(StepsRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(OxygenSaturationRecord::class),
            HealthPermission.getWritePermission(OxygenSaturationRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getWritePermission(WeightRecord::class),
            HealthPermission.getReadPermission(BodyFatRecord::class),
            HealthPermission.getWritePermission(BodyFatRecord::class),
            HealthPermission.getReadPermission(HeightRecord::class),
            HealthPermission.getWritePermission(HeightRecord::class),
            HealthPermission.getReadPermission(BodyTemperatureRecord::class),
            HealthPermission.getWritePermission(BodyTemperatureRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onAuthorizeHuawei: () -> Unit,
    onRequestHealthPermissions: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("华为健康桥") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "将华为运动健康数据同步到 Health Connect",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // --- 步骤 1: 授权华为 ---
            StepButton(
                step = "1",
                label = "授权华为运动健康",
                isDone = uiState.huaweiAuthState == HuaweiAuthState.Authorized,
                isInProgress = uiState.huaweiAuthState == HuaweiAuthState.Authorizing,
                enabled = uiState.syncState != SyncState.Syncing,
                onClick = onAuthorizeHuawei
            )

            // --- 步骤 2: 授权 Health Connect ---
            StepButton(
                step = "2",
                label = "授权 Health Connect",
                isDone = uiState.healthConnectState == HealthConnectState.Granted,
                isInProgress = uiState.healthConnectState == HealthConnectState.Granting,
                enabled = uiState.syncState != SyncState.Syncing,
                onClick = onRequestHealthPermissions
            )

            // --- 步骤 3: 开始同步 ---
            Button(
                onClick = { viewModel.syncData() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.syncState != SyncState.Syncing
            ) {
                Text("3. 开始同步")
            }

            Spacer(Modifier.height(8.dp))

            // --- 同步状态区域 ---
            SyncStatusSection(uiState)

            // --- 结果摘要 ---
            if (uiState.syncResult != null) {
                SyncResultCard(uiState.syncResult!!)
            }

            // --- 刷新按钮 ---
            OutlinedButton(
                onClick = { viewModel.refreshSync() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.syncState != SyncState.Syncing
            ) {
                Text("刷新同步")
            }
        }
    }
}

@Composable
private fun StepButton(
    step: String,
    label: String,
    isDone: Boolean,
    isInProgress: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled && !isDone,
        colors = if (isDone) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            ButtonDefaults.buttonColors()
        }
    ) {
        if (isInProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.width(8.dp))
        }
        Text("$step. $label")
        if (isDone) {
            Spacer(Modifier.width(8.dp))
            Text("✓", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SyncStatusSection(uiState: MainUiState) {
    when (uiState.syncState) {
        SyncState.Syncing -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { if (uiState.syncProgress > 0) uiState.syncProgress.toFloat() / 4f else 0f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    if (uiState.syncedTypes.isEmpty()) "正在同步健康数据..."
                    else "已同步: ${uiState.syncedTypes.joinToString(" ")} (${uiState.syncProgress}/4)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        SyncState.Error -> {
            val msg = uiState.syncError ?: "同步失败"
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    msg,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        SyncState.Idle, SyncState.Success -> { /* 空闲或成功时此处无特殊状态 */ }
    }
}

@Composable
private fun SyncResultCard(result: SyncResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "同步完成",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(4.dp))
            SyncResultRow("睡眠", result.sleep)
            SyncResultRow("心率", result.heartRate)
            SyncResultRow("步数", result.steps)
            SyncResultRow("运动", result.exercise)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                "共同步 ${result.total} 条记录",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun SyncResultRow(label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text("${count} 条", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

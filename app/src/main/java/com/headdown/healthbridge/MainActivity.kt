package com.headdown.healthbridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.work.*
import com.headdown.healthbridge.huawei.HuaweiConfig
import com.headdown.healthbridge.huawei.HuaweiHealthClient
import com.headdown.healthbridge.healthconnect.HealthConnectWriter
import com.headdown.healthbridge.sync.SyncWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context = this) }
    private lateinit var huaweiClient: HuaweiHealthClient
    private lateinit var healthConnectWriter: HealthConnectWriter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        huaweiClient = HuaweiHealthClient(this)
        healthConnectWriter = HealthConnectWriter(healthConnectClient)

        // 检查 OAuth 回调
        handleOAuthCallback(intent)

        // 启动定时同步任务
        schedulePeriodicSync()

        setContent {
            MaterialTheme {
                MainScreen(
                    onSync = { syncData() },
                    onAuthorizeHuawei = { startHuaweiAuth() },
                    onRequestHealthPermissions = { requestHealthPermissions() }
                )
            }
        }
    }

    private fun handleOAuthCallback(intent: android.content.Intent) {
        intent.data?.let { uri ->
            if (uri.scheme == "com.headdown.healthbridge" && uri.host == "oauth") {
                val code = uri.getQueryParameter("code")
                if (code != null) {
                    huaweiClient.exchangeCodeForToken(code)
                }
            }
        }
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

    private fun syncData() {
        // TODO: 实现完整同步流程
        // 1. 检查华为 Token 是否有效
        // 2. 从华为 API 拉取最新数据
        // 3. 去重（根据时间戳）
        // 4. 写入 Health Connect
    }

    private fun startHuaweiAuth() {
        val authUrl = huaweiClient.getAuthorizationUrl()
        // TODO: 启动浏览器进行 OAuth 授权
    }

    private fun requestHealthPermissions() {
        val permissions = setOf(
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

        val launcher = registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { granted ->
            if (granted.containsAll(permissions)) {
                // 权限已授予，开始同步
                syncData()
            }
        }
        launcher.launch(permissions)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onSync: () -> Unit,
    onAuthorizeHuawei: () -> Unit,
    onRequestHealthPermissions: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("华为健康桥") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("将华为运动健康数据同步到 Health Connect")
            Spacer(Modifier.height(16.dp))

            Button(onClick = onAuthorizeHuawei, modifier = Modifier.fillMaxWidth()) {
                Text("1. 授权华为运动健康")
            }

            Button(onClick = onRequestHealthPermissions, modifier = Modifier.fillMaxWidth()) {
                Text("2. 授权 Health Connect")
            }

            Button(onClick = onSync, modifier = Modifier.fillMaxWidth()) {
                Text("3. 开始同步")
            }
        }
    }
}

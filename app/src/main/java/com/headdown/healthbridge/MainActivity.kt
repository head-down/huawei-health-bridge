package com.headdown.healthbridge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.headdown.healthbridge.sync.SyncWorker
import com.headdown.healthbridge.ui.MainScreen
import com.headdown.healthbridge.ui.theme.HealthBridgeTheme
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
        } else {
            mainViewModel.onHealthConnectDenied()
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
            HealthBridgeTheme {
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

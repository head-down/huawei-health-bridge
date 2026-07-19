package com.headdown.healthbridge.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.headdown.healthbridge.HealthConnectState
import com.headdown.healthbridge.HuaweiAuthState
import com.headdown.healthbridge.MainUiState
import com.headdown.healthbridge.MainViewModel
import com.headdown.healthbridge.SyncState
import com.headdown.healthbridge.data.SyncResult

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
            TopAppBar(
                title = { Text("华为健康桥") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderSection()

            StepperSection(
                uiState = uiState,
                onAuthorizeHuawei = onAuthorizeHuawei,
                onRequestHealthPermissions = onRequestHealthPermissions,
                onSync = { viewModel.syncData() }
            )

            SyncStatusSection(uiState)

            AnimatedVisibility(
                visible = uiState.syncResult != null,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn()
            ) {
                uiState.syncResult?.let { SyncResultCard(it) }
            }

            OutlinedButton(
                onClick = { viewModel.refreshSync() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.syncState != SyncState.Syncing,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("刷新同步")
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = "华为健康桥",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "将华为运动健康数据安全同步到 Google Health Connect",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun StepperSection(
    uiState: MainUiState,
    onAuthorizeHuawei: () -> Unit,
    onRequestHealthPermissions: () -> Unit,
    onSync: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        StepCard(
            icon = Icons.Default.Cloud,
            title = "授权华为运动健康",
            description = "登录华为账号并授权读取运动健康数据",
            state = when (uiState.huaweiAuthState) {
                HuaweiAuthState.Idle -> StepState.Todo
                HuaweiAuthState.Authorizing -> StepState.Doing
                HuaweiAuthState.Authorized -> StepState.Done
            },
            buttonText = if (uiState.huaweiAuthState == HuaweiAuthState.Authorized) "已完成" else "去授权",
            onButtonClick = onAuthorizeHuawei,
            enabled = uiState.syncState != SyncState.Syncing,
            isLast = false
        )

        StepCard(
            icon = Icons.Default.Favorite,
            title = "授权 Health Connect",
            description = "允许写入睡眠、心率、步数等健康数据",
            state = when (uiState.healthConnectState) {
                HealthConnectState.Idle -> StepState.Todo
                HealthConnectState.Granting -> StepState.Doing
                HealthConnectState.Granted -> StepState.Done
            },
            buttonText = if (uiState.healthConnectState == HealthConnectState.Granted) "已完成" else "去授权",
            onButtonClick = onRequestHealthPermissions,
            enabled = uiState.syncState != SyncState.Syncing,
            isLast = false
        )

        StepCard(
            icon = Icons.Default.Sync,
            title = "开始同步",
            description = "一键将华为数据同步到 Health Connect",
            state = when {
                uiState.syncState == SyncState.Syncing -> StepState.Doing
                uiState.syncState == SyncState.Success -> StepState.Done
                else -> StepState.Todo
            },
            buttonText = when (uiState.syncState) {
                SyncState.Syncing -> "同步中"
                SyncState.Success -> "同步完成"
                else -> "开始同步"
            },
            onButtonClick = onSync,
            enabled = uiState.syncState != SyncState.Syncing &&
                    uiState.huaweiAuthState == HuaweiAuthState.Authorized &&
                    uiState.healthConnectState == HealthConnectState.Granted,
            isLast = true
        )
    }
}

private enum class StepState { Todo, Doing, Done }

@Composable
private fun StepCard(
    icon: ImageVector,
    title: String,
    description: String,
    state: StepState,
    buttonText: String,
    onButtonClick: () -> Unit,
    enabled: Boolean,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            StepIndicator(state = state, icon = icon)
            if (!isLast) {
                Spacer(Modifier.height(4.dp))
                VerticalDivider(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f),
                    color = if (state == StepState.Done) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                )
            }
        }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isLast) 0.dp else 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onButtonClick,
                    enabled = enabled && state != StepState.Done,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state == StepState.Doing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(buttonText)
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(
    state: StepState,
    icon: ImageVector
) {
    val containerColor = when (state) {
        StepState.Todo -> MaterialTheme.colorScheme.surfaceVariant
        StepState.Doing -> MaterialTheme.colorScheme.secondaryContainer
        StepState.Done -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when (state) {
        StepState.Todo -> MaterialTheme.colorScheme.onSurfaceVariant
        StepState.Doing -> MaterialTheme.colorScheme.onSecondaryContainer
        StepState.Done -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(40.dp)
    ) {
        if (state == StepState.Doing) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .background(containerColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                StepState.Done -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已完成",
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                else -> Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SyncStatusSection(uiState: MainUiState) {
    AnimatedVisibility(
        visible = uiState.syncState == SyncState.Syncing || uiState.syncState == SyncState.Error,
        enter = fadeIn() + expandVertically()
    ) {
        when (uiState.syncState) {
            SyncState.Syncing -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (uiState.syncedTypes.isEmpty()) {
                                "正在同步健康数据..."
                            } else {
                                "已同步: ${uiState.syncedTypes.joinToString(" ") { it.label }} (${uiState.syncProgress}/4)"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            SyncState.Error -> {
                val msg = uiState.syncError ?: "同步失败"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            else -> {}
        }
    }
}

@Composable
private fun SyncResultCard(result: SyncResult) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "同步完成",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResultGridItem(
                    icon = Icons.Default.Bedtime,
                    label = "睡眠",
                    count = result.sleep,
                    modifier = Modifier.weight(1f)
                )
                ResultGridItem(
                    icon = Icons.Default.MonitorHeart,
                    label = "心率",
                    count = result.heartRate,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResultGridItem(
                    icon = Icons.Default.DirectionsWalk,
                    label = "步数",
                    count = result.steps,
                    modifier = Modifier.weight(1f)
                )
                ResultGridItem(
                    icon = Icons.Default.FitnessCenter,
                    label = "运动",
                    count = result.exercise,
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = "共同步 ${result.total} 条记录",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun ResultGridItem(
    icon: ImageVector,
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

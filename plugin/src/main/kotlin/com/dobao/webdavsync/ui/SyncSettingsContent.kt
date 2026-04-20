package com.dobao.webdavsync.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SyncSettingsContent(
    webDAVUrl: String,
    onWebDAVUrlChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    syncReadingHistory: Boolean,
    onSyncReadingHistoryChange: (Boolean) -> Unit,
    syncBookshelf: Boolean,
    onSyncBookshelfChange: (Boolean) -> Unit,
    syncSettings: Boolean,
    onSyncSettingsChange: (Boolean) -> Unit,
    autoSyncEnabled: Boolean,
    onAutoSyncEnabledChange: (Boolean) -> Unit,
    autoSyncInterval: Int,
    onAutoSyncIntervalChange: (Int) -> Unit,
    syncOnWifiOnly: Boolean,
    onSyncOnWifiOnlyChange: (Boolean) -> Unit,
    lastSyncTime: String,
    isSyncing: Boolean,
    syncStatus: String,
    syncError: String?,
    onManualSync: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // WebDAV 服务器配置
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "WebDAV 服务器",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = webDAVUrl,
                    onValueChange = onWebDAVUrlChange,
                    label = { Text("服务器地址") },
                    placeholder = { Text("https://your-server.com/webdav") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text("用户名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("密码") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        }

        // 同步选项
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "同步内容",
                    style = MaterialTheme.typography.titleMedium
                )

                SwitchRow(
                    label = "阅读记录",
                    description = "同步阅读历史和进度",
                    checked = syncReadingHistory,
                    onCheckedChange = onSyncReadingHistoryChange
                )

                SwitchRow(
                    label = "书架",
                    description = "同步书架中的书籍",
                    checked = syncBookshelf,
                    onCheckedChange = onSyncBookshelfChange
                )

                SwitchRow(
                    label = "软件设置",
                    description = "同步应用配置",
                    checked = syncSettings,
                    onCheckedChange = onSyncSettingsChange
                )
            }
        }

        // 自动同步设置
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "自动同步",
                    style = MaterialTheme.typography.titleMedium
                )

                SwitchRow(
                    label = "启用自动同步",
                    description = "定时自动同步数据",
                    checked = autoSyncEnabled,
                    onCheckedChange = onAutoSyncEnabledChange
                )

                if (autoSyncEnabled) {
                    Text(
                        text = "同步间隔: $autoSyncInterval 分钟",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = autoSyncInterval.toFloat(),
                        onValueChange = { onAutoSyncIntervalChange(it.toInt()) },
                        valueRange = 5f..120f,
                        steps = 22
                    )

                    SwitchRow(
                        label = "仅 Wi-Fi 同步",
                        description = "仅在连接 Wi-Fi 时自动同步",
                        checked = syncOnWifiOnly,
                        onCheckedChange = onSyncOnWifiOnlyChange
                    )
                }
            }
        }

        // 同步状态和手动同步
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "同步状态",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "上次同步: $lastSyncTime",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (syncStatus.isNotBlank()) {
                    Text(
                        text = syncStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (syncError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                if (syncError != null) {
                    Text(
                        text = syncError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onManualSync,
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp).width(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("同步中...")
                    } else {
                        Text("立即同步")
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

package com.dobao.webdavsync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dobao.webdavsync.WebDAVSyncPlugin
import java.text.SimpleDateFormat
import java.util.*

/**
 * 同步设置页面
 */
@Composable
fun SyncSettingsContent(
    paddingValues: PaddingValues,
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
    lastSyncTime: Long,
    isSyncing: Boolean,
    syncStatus: String,
    syncError: String?,
    onManualSync: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var showAdvancedSettings by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题
        Text(
            text = "WebDAV 云同步",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        // WebDAV 服务器配置
        SettingsSection(title = "服务器配置") {
            OutlinedTextField(
                value = webDAVUrl,
                onValueChange = onWebDAVUrlChange,
                label = { Text("服务器地址") },
                placeholder = { Text("https://dav.example.com") },
                leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = { Text("用户名") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("密码") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "切换密码可见性"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        
        // 同步内容选择
        SettingsSection(title = "同步内容") {
            SettingsSwitch(
                title = "阅读记录",
                description = "同步阅读进度、章节位置等",
                checked = syncReadingHistory,
                onCheckedChange = onSyncReadingHistoryChange,
                icon = Icons.Default.History
            )
            
            HorizontalDivider()
            
            SettingsSwitch(
                title = "书架内容",
                description = "同步书架中的书籍",
                checked = syncBookshelf,
                onCheckedChange = onSyncBookshelfChange,
                icon = Icons.Default.LibraryBooks
            )
            
            HorizontalDivider()
            
            SettingsSwitch(
                title = "软件设置",
                description = "同步阅读器设置和偏好",
                checked = syncSettings,
                onCheckedChange = onSyncSettingsChange,
                icon = Icons.Default.Settings
            )
        }
        
        // 自动同步设置
        SettingsSection(title = "自动同步") {
            SettingsSwitch(
                title = "启用自动同步",
                description = "按设定间隔自动同步数据",
                checked = autoSyncEnabled,
                onCheckedChange = onAutoSyncEnabledChange,
                icon = Icons.Default.Sync
            )
            
            if (autoSyncEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "同步间隔: $autoSyncInterval 分钟",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 40.dp)
                )
                
                Slider(
                    value = autoSyncInterval.toFloat(),
                    onValueChange = { onAutoSyncIntervalChange(it.toInt()) },
                    valueRange = 5f..120f,
                    steps = 22,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                SettingsSwitch(
                    title = "仅在 Wi-Fi 下同步",
                    description = "节省移动流量",
                    checked = syncOnWifiOnly,
                    onCheckedChange = onSyncOnWifiOnlyChange,
                    icon = Icons.Default.Wifi
                )
            }
        }
        
        // 同步状态
        if (lastSyncTime > 0 || syncStatus.isNotBlank()) {
            SettingsSection(title = "同步状态") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "上次同步",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = formatTime(lastSyncTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (syncStatus.isNotBlank()) {
                        Text(
                            text = syncStatus,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (syncError != null) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                if (syncError != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = syncError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
        
        // 手动同步按钮
        Button(
            onClick = onManualSync,
            enabled = !isSyncing && webDAVUrl.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("同步中...")
            } else {
                Icon(Icons.Default.CloudSync, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("立即同步")
            }
        }
        
        // 高级设置折叠区域
        TextButton(
            onClick = { showAdvancedSettings = !showAdvancedSettings },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (showAdvancedSettings) "收起高级设置" else "显示高级设置")
        }
        
        if (showAdvancedSettings) {
            SettingsSection(title = "高级设置") {
                Text(
                    text = "基础路径: /LightNovelReader",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "同步文件:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• reading_history/reading_history.json",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Text(
                    text = "• bookshelf/bookshelf.json",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Text(
                    text = "• settings/settings.json",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * 设置区块
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

/**
 * 设置开关项
 */
@Composable
private fun SettingsSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * 格式化时间
 */
private fun formatTime(timestamp: Long): String {
    if (timestamp <= 0) return "从未同步"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

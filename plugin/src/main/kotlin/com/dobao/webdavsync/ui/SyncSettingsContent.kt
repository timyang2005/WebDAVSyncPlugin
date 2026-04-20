package com.dobao.webdavsync.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SyncSettingsContent(
    webDAVUrl: String,
    username: String,
    password: String,
    syncReadingHistory: Boolean,
    syncBookshelf: Boolean,
    syncSettings: Boolean,
    autoSyncEnabled: Boolean,
    statusMessage: String,
    onWebDAVUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSyncReadingHistoryChange: (Boolean) -> Unit,
    onSyncBookshelfChange: (Boolean) -> Unit,
    onSyncSettingsChange: (Boolean) -> Unit,
    onAutoSyncChange: (Boolean) -> Unit,
    onSyncClick: () -> Unit,
    onSaveConfig: () -> Unit
) {
    var pwdVisible by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("WebDAV Cloud Sync", MaterialTheme.typography.headlineSmall, Modifier.padding(bottom = 16.dp))

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp)) {
                Text("Server Configuration", MaterialTheme.typography.titleMedium, Modifier.padding(bottom = 12.dp))
                OutlinedTextField(webDAVUrl, onWebDAVUrlChange, label = { Text("Server URL") }, placeholder = { Text("https://your-server.com/webdav") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(username, onUsernameChange, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(password, onPasswordChange, label = { Text("Password") }, singleLine = true, visualTransformation = if (pwdVisible) VisualTransformation.None else PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp)) {
                Text("Sync Options", MaterialTheme.typography.titleMedium, Modifier.padding(bottom = 12.dp))
                SwitchRow("Reading History", "Sync reading progress and positions", syncReadingHistory, onSyncReadingHistoryChange)
                Spacer(Modifier.height(8.dp))
                SwitchRow("Bookshelf", "Sync bookshelf content", syncBookshelf, onSyncBookshelfChange)
                Spacer(Modifier.height(8.dp))
                SwitchRow("App Settings", "Sync app configuration", syncSettings, onSyncSettingsChange)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SwitchRow("Auto Sync", "Automatically sync on startup", autoSyncEnabled, onAutoSyncChange)
            }
        }

        Spacer(Modifier.height(16.dp))

        if (statusMessage.isNotBlank()) {
            Text(statusMessage, MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onSaveConfig, modifier = Modifier.weight(1f)) { Text("Save") }
            Button(onClick = onSyncClick, modifier = Modifier.weight(1f)) { Text("Sync Now") }
        }
    }
}

@Composable
private fun SwitchRow(label: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, MaterialTheme.typography.bodyLarge)
            Text(desc, MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

package com.dobao.webdavsync.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("WebDAV Sync Settings", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = webDAVUrl,
            onValueChange = onWebDAVUrlChange,
            label = { Text("WebDAV URL") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onSyncClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sync Now")
        }
        
        if (statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(statusMessage, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

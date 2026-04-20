package com.dobao.webdavsync

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.nightfish.lightnovelreader.api.plugin.LightNovelReaderPlugin
import io.nightfish.lightnovelreader.api.plugin.Plugin
import io.nightfish.lightnovelreader.api.userdata.UserDataRepositoryApi
import io.nightfish.lightnovelreader.api.userdata.UserDataDaoApi
import io.nightfish.lightnovelreader.api.book.BookRepositoryApi
import io.nightfish.lightnovelreader.api.bookshelf.BookshelfRepositoryApi
import io.nightfish.lightnovelreader.api.LocalBookDataSourceApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.dobao.webdavsync.data.SyncType
import com.dobao.webdavsync.data.WebDAVConfig
import com.dobao.webdavsync.sync.SyncManager
import com.dobao.webdavsync.ui.SyncSettingsContent
import kotlinx.coroutines.launch

@Plugin(
    version = 1,
    name = "WebDAV Sync",
    versionName = "1.0.0",
    author = "DoBao",
    description = "WebDAV 云同步插件，支持同步阅读记录、书架和软件设置",
    updateUrl = "https://github.com/dobao/WebDAVSyncPlugin",
    apiVersion = 1
)
class WebDAVSyncPlugin(
    private val userDataRepositoryApi: UserDataRepositoryApi,
    private val userDataDaoApi: UserDataDaoApi,
    private val bookRepositoryApi: BookRepositoryApi,
    private val bookshelfRepositoryApi: BookshelfRepositoryApi,
    private val localBookDataSourceApi: LocalBookDataSourceApi
) : LightNovelReaderPlugin() {
    
    private var syncManager: SyncManager? = null
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
    
    companion object {
        const val PREF_WEBDAV_URL = "webdav_url"
        const val PREF_WEBDAV_USERNAME = "webdav_username"
        const val PREF_WEBDAV_PASSWORD = "webdav_password"
        const val PREF_SYNC_READING_HISTORY = "sync_reading_history"
        const val PREF_SYNC_BOOKSHELF = "sync_bookshelf"
        const val PREF_SYNC_SETTINGS = "sync_settings"
        const val PREF_AUTO_SYNC_ENABLED = "auto_sync_enabled"
        const val PREF_AUTO_SYNC_INTERVAL = "auto_sync_interval"
        const val PREF_LAST_SYNC_TIME = "last_sync_time"
        const val PREF_SYNC_ON_WIFI_ONLY = "sync_wifi_only"
        
        const val DEFAULT_SYNC_INTERVAL = 30 // minutes
        const val SYNC_BASE_PATH = "/LightNovelReader"
    }
    
    override fun onLoad() {
        // 初始化同步管理器
        val config = loadWebDAVConfig()
        syncManager = SyncManager(
            webDAVConfig = config,
            userDataRepositoryApi = userDataRepositoryApi,
            userDataDaoApi = userDataDaoApi,
            bookRepositoryApi = bookRepositoryApi,
            bookshelfRepositoryApi = bookshelfRepositoryApi,
            localBookDataSourceApi = localBookDataSourceApi
        )
        
        // 检查是否启用自动同步
        scope.launch {
            userDataRepositoryApi
                .booleanUserData(PREF_AUTO_SYNC_ENABLED)
                .getFlowWithDefault(false)
                .collect { enabled ->
                    if (enabled) {
                        startAutoSync()
                    }
                }
        }
    }
    
    private fun loadWebDAVConfig(): WebDAVConfig {
        return WebDAVConfig(
            url = "",
            username = "",
            password = ""
        )
    }
    
    private fun startAutoSync() {
        scope.launch {
            userDataRepositoryApi
                .intUserData(PREF_AUTO_SYNC_INTERVAL)
                .getFlowWithDefault(DEFAULT_SYNC_INTERVAL)
                .collect { minutes ->
                    // 定时执行同步
                    syncManager?.let { _ ->
                        performSync(
                            readingHistory = isSyncEnabled(PREF_SYNC_READING_HISTORY),
                            bookshelf = isSyncEnabled(PREF_SYNC_BOOKSHELF),
                            settings = isSyncEnabled(PREF_SYNC_SETTINGS)
                        )
                    }
                }
        }
    }
    
    private suspend fun isSyncEnabled(key: String): Boolean {
        var result = false
        userDataRepositoryApi.booleanUserData(key).getFlowWithDefault(true).collect {
            result = it
        }
        return result
    }
    
    @Composable
    override fun PageContent(paddingValues: PaddingValues) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        
        // 状态
        var webDAVUrl by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var syncReadingHistory by remember { mutableStateOf(true) }
        var syncBookshelf by remember { mutableStateOf(true) }
        var syncSettings by remember { mutableStateOf(true) }
        var autoSyncEnabled by remember { mutableStateOf(false) }
        var autoSyncInterval by remember { mutableIntStateOf(DEFAULT_SYNC_INTERVAL) }
        var syncOnWifiOnly by remember { mutableStateOf(true) }
        var lastSyncTime by remember { mutableStateOf(0L) }
        var isSyncing by remember { mutableStateOf(false) }
        var syncStatus by remember { mutableStateOf("") }
        var syncError by remember { mutableStateOf<String?>(null) }
        
        // 加载配置
        LaunchedEffect(Unit) {
            webDAVUrl = loadStringPref(PREF_WEBDAV_URL)
            username = loadStringPref(PREF_WEBDAV_USERNAME)
            password = loadStringPref(PREF_WEBDAV_PASSWORD)
            syncReadingHistory = loadBooleanPref(PREF_SYNC_READING_HISTORY, true)
            syncBookshelf = loadBooleanPref(PREF_SYNC_BOOKSHELF, true)
            syncSettings = loadBooleanPref(PREF_SYNC_SETTINGS, true)
            autoSyncEnabled = loadBooleanPref(PREF_AUTO_SYNC_ENABLED, false)
            autoSyncInterval = loadIntPref(PREF_AUTO_SYNC_INTERVAL, DEFAULT_SYNC_INTERVAL)
            syncOnWifiOnly = loadBooleanPref(PREF_SYNC_ON_WIFI_ONLY, true)
            lastSyncTime = loadLongPref(PREF_LAST_SYNC_TIME, 0L)
        }
        
        SyncSettingsContent(
            paddingValues = paddingValues,
            webDAVUrl = webDAVUrl,
            onWebDAVUrlChange = { 
                webDAVUrl = it
                scope.launch { saveStringPref(PREF_WEBDAV_URL, it) }
            },
            username = username,
            onUsernameChange = {
                username = it
                scope.launch { saveStringPref(PREF_WEBDAV_USERNAME, it) }
            },
            password = password,
            onPasswordChange = {
                password = it
                scope.launch { saveStringPref(PREF_WEBDAV_PASSWORD, it) }
            },
            syncReadingHistory = syncReadingHistory,
            onSyncReadingHistoryChange = {
                syncReadingHistory = it
                scope.launch { saveBooleanPref(PREF_SYNC_READING_HISTORY, it) }
            },
            syncBookshelf = syncBookshelf,
            onSyncBookshelfChange = {
                syncBookshelf = it
                scope.launch { saveBooleanPref(PREF_SYNC_BOOKSHELF, it) }
            },
            syncSettings = syncSettings,
            onSyncSettingsChange = {
                syncSettings = it
                scope.launch { saveBooleanPref(PREF_SYNC_SETTINGS, it) }
            },
            autoSyncEnabled = autoSyncEnabled,
            onAutoSyncEnabledChange = {
                autoSyncEnabled = it
                scope.launch { saveBooleanPref(PREF_AUTO_SYNC_ENABLED, it) }
            },
            autoSyncInterval = autoSyncInterval,
            onAutoSyncIntervalChange = {
                autoSyncInterval = it
                scope.launch { saveIntPref(PREF_AUTO_SYNC_INTERVAL, it) }
            },
            syncOnWifiOnly = syncOnWifiOnly,
            onSyncOnWifiOnlyChange = {
                syncOnWifiOnly = it
                scope.launch { saveBooleanPref(PREF_SYNC_ON_WIFI_ONLY, it) }
            },
            lastSyncTime = lastSyncTime,
            isSyncing = isSyncing,
            syncStatus = syncStatus,
            syncError = syncError,
            onManualSync = {
                scope.launch {
                    performManualSync(
                        webDAVUrl, username, password,
                        syncReadingHistory, syncBookshelf, syncSettings
                    ) { status, error ->
                        syncStatus = status
                        syncError = error
                        if (error == null) {
                            lastSyncTime = System.currentTimeMillis()
                            scope.launch { saveLongPref(PREF_LAST_SYNC_TIME, lastSyncTime) }
                        }
                    }
                }
            }
        )
    }
    
    private suspend fun loadStringPref(key: String): String {
        var result = ""
        userDataRepositoryApi.stringUserData(key).getFlowWithDefault("").collect {
            result = it
        }
        return result
    }
    
    private suspend fun loadBooleanPref(key: String, default: Boolean): Boolean {
        var result = default
        userDataRepositoryApi.booleanUserData(key).getFlowWithDefault(default).collect {
            result = it
        }
        return result
    }
    
    private suspend fun loadIntPref(key: String, default: Int): Int {
        var result = default
        userDataRepositoryApi.intUserData(key).getFlowWithDefault(default).collect {
            result = it
        }
        return result
    }
    
    private suspend fun loadLongPref(key: String, default: Long): Long {
        var result = default
        userDataRepositoryApi.longUserData(key).getFlowWithDefault(default).collect {
            result = it
        }
        return result
    }
    
    private suspend fun saveStringPref(key: String, value: String) {
        userDataRepositoryApi.stringUserData(key).set(value)
    }
    
    private suspend fun saveBooleanPref(key: String, value: Boolean) {
        userDataRepositoryApi.booleanUserData(key).set(value)
    }
    
    private suspend fun saveIntPref(key: String, value: Int) {
        userDataRepositoryApi.intUserData(key).set(value)
    }
    
    private suspend fun saveLongPref(key: String, value: Long) {
        userDataRepositoryApi.longUserData(key).set(value)
    }
    
    private suspend fun performManualSync(
        webDAVUrl: String,
        username: String,
        password: String,
        syncReadingHistory: Boolean,
        syncBookshelf: Boolean,
        syncSettings: Boolean,
        onStatus: (String, String?) -> Unit
    ) {
        if (webDAVUrl.isBlank()) {
            onStatus("同步失败", "请先配置 WebDAV 服务器地址")
            return
        }
        
        onStatus("正在同步...", null)
        
        val config = WebDAVConfig(
            url = webDAVUrl,
            username = username,
            password = password
        )
        
        syncManager?.updateConfig(config)
        
        val syncTypes = mutableListOf<SyncType>()
        if (syncReadingHistory) syncTypes.add(SyncType.READING_HISTORY)
        if (syncBookshelf) syncTypes.add(SyncType.BOOKSHELF)
        if (syncSettings) syncTypes.add(SyncType.SETTINGS)
        
        try {
            syncManager?.performSync(syncTypes) { status ->
                onStatus(status, null)
            }
            onStatus("同步完成", null)
        } catch (e: Exception) {
            onStatus("同步失败", e.message ?: "未知错误")
        }
    }
    
    private suspend fun performSync(
        readingHistory: Boolean,
        bookshelf: Boolean,
        settings: Boolean
    ) {
        val syncTypes = mutableListOf<SyncType>()
        if (readingHistory) syncTypes.add(SyncType.READING_HISTORY)
        if (bookshelf) syncTypes.add(SyncType.BOOKSHELF)
        if (settings) syncTypes.add(SyncType.SETTINGS)
        
        syncManager?.performSync(syncTypes) { }
    }
}

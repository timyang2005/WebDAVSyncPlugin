package com.dobao.webdavsync

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.nightfish.lightnovelreader.api.plugin.LightNovelReaderPlugin
import io.nightfish.lightnovelreader.api.plugin.Plugin
import io.nightfish.lightnovelreader.api.userdata.UserDataRepositoryApi
import io.nightfish.lightnovelreader.api.userdata.UserDataDaoApi
import io.nightfish.lightnovelreader.api.book.BookRepositoryApi
import io.nightfish.lightnovelreader.api.bookshelf.BookshelfRepositoryApi
import io.nightfish.lightnovelreader.api.book.LocalBookDataSourceApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    updateUrl = "https://github.com/timyang2005/WebDAVSyncPlugin",
    apiVersion = 2
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
                    // 定期执行同步
                    syncManager?.let { manager ->
                        performSync(
                            readingHistory = true,
                            bookshelf = true,
                            settings = true
                        )
                    }
                }
        }
    }

    @Composable
    override fun PageContent(paddingValues: PaddingValues) {
        var webDAVUrl by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var syncReadingHistory by remember { mutableStateOf(true) }
        var syncBookshelf by remember { mutableStateOf(true) }
        var syncSettings by remember { mutableStateOf(true) }
        var autoSyncEnabled by remember { mutableStateOf(false) }
        var statusMessage by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            webDAVUrl = userDataRepositoryApi.stringUserData(PREF_WEBDAV_URL).get() ?: ""
            username = userDataRepositoryApi.stringUserData(PREF_WEBDAV_USERNAME).get() ?: ""
            password = userDataRepositoryApi.stringUserData(PREF_WEBDAV_PASSWORD).get() ?: ""
            syncReadingHistory = userDataRepositoryApi.booleanUserData(PREF_SYNC_READING_HISTORY).get() ?: true
            syncBookshelf = userDataRepositoryApi.booleanUserData(PREF_SYNC_BOOKSHELF).get() ?: true
            syncSettings = userDataRepositoryApi.booleanUserData(PREF_SYNC_SETTINGS).get() ?: true
            autoSyncEnabled = userDataRepositoryApi.booleanUserData(PREF_AUTO_SYNC_ENABLED).get() ?: false
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                SyncSettingsContent(
                    webDAVUrl = webDAVUrl,
                    username = username,
                    password = password,
                    syncReadingHistory = syncReadingHistory,
                    syncBookshelf = syncBookshelf,
                    syncSettings = syncSettings,
                    autoSyncEnabled = autoSyncEnabled,
                    statusMessage = statusMessage,
                    onWebDAVUrlChange = { webDAVUrl = it },
                    onUsernameChange = { username = it },
                    onPasswordChange = { password = it },
                    onSyncReadingHistoryChange = { syncReadingHistory = it },
                    onSyncBookshelfChange = { syncBookshelf = it },
                    onSyncSettingsChange = { syncSettings = it },
                    onAutoSyncChange = { autoSyncEnabled = it },
                    onSyncClick = {
                        scope.launch {
                            statusMessage = "正在同步..."
                            performManualSync(
                                webDAVUrl, username, password,
                                syncReadingHistory, syncBookshelf, syncSettings
                            ) { msg -> statusMessage = msg }
                        }
                    },
                    onSaveConfig = {
                        scope.launch {
                            userDataRepositoryApi.stringUserData(PREF_WEBDAV_URL).set(webDAVUrl)
                            userDataRepositoryApi.stringUserData(PREF_WEBDAV_USERNAME).set(username)
                            userDataRepositoryApi.stringUserData(PREF_WEBDAV_PASSWORD).set(password)
                            userDataRepositoryApi.booleanUserData(PREF_SYNC_READING_HISTORY).set(syncReadingHistory)
                            userDataRepositoryApi.booleanUserData(PREF_SYNC_BOOKSHELF).set(syncBookshelf)
                            userDataRepositoryApi.booleanUserData(PREF_SYNC_SETTINGS).set(syncSettings)
                            userDataRepositoryApi.booleanUserData(PREF_AUTO_SYNC_ENABLED).set(autoSyncEnabled)
                            statusMessage = "配置已保存"
                        }
                    }
                )
            }
        }
    }

    private suspend fun performManualSync(
        webDAVUrl: String,
        username: String,
        password: String,
        syncReadingHistory: Boolean,
        syncBookshelf: Boolean,
        syncSettings: Boolean,
        onStatus: (String) -> Unit
    ) {
        if (webDAVUrl.isBlank()) {
            onStatus("同步失败: 请先配置 WebDAV 服务器地址")
            return
        }

        onStatus("正在同步...")

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
                onStatus(status)
            }
            onStatus("同步完成")
        } catch (e: Exception) {
            onStatus("同步失败: ${e.message ?: "未知错误"}")
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

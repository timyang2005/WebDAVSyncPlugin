package com.dobao.webdavsync

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dobao.webdavsync.data.SyncData
import com.dobao.webdavsync.data.SyncType
import com.dobao.webdavsync.data.WebDAVConfig
import com.dobao.webdavsync.sync.SyncManager
import com.dobao.webdavsync.ui.SyncSettingsContent
import io.nightfish.lightnovelreader.api.plugin.LightNovelReaderPlugin
import io.nightfish.lightnovelreader.api.plugin.Plugin
import io.nightfish.lightnovelreader.api.userdata.UserDataRepositoryApi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("unused")
@Plugin(
    version = BuildConfig.VERSION_CODE,
    name = "WebDAV Sync",
    versionName = BuildConfig.VERSION_NAME,
    author = "DoBao",
    description = "WebDAV 云同步插件，支持同步阅读记录、书架和软件设置",
    updateUrl = "https://github.com/timyang2005/WebDAVSyncPlugin",
    apiVersion = 2
)
class WebDAVSyncPlugin(
    private val userDataRepositoryApi: UserDataRepositoryApi
) : LightNovelReaderPlugin() {

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
        const val DEFAULT_SYNC_INTERVAL = 30
        const val SYNC_BASE_PATH = "/LightNovelReader"
    }

    private var syncManager: SyncManager? = null

    override fun onLoad() {
        Log.i("WebDAVSyncPlugin", "WebDAV Sync 插件已加载")
    }

    @Composable
    override fun PageContent(paddingValues: PaddingValues) {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

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
        var lastSyncTime by remember { mutableLongStateOf(0L) }
        var isSyncing by remember { mutableStateOf(false) }
        var syncStatus by remember { mutableStateOf("") }
        var syncError by remember { mutableStateOf<String?>(null) }

        // 初始化 SyncManager
        if (syncManager == null) {
            syncManager = SyncManager(
                webDAVConfig = WebDAVConfig(webDAVUrl, username, password),
                userDataRepositoryApi = userDataRepositoryApi
            )
        }

        // 加载配置
        val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp)),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "WebDAV 同步设置",
                style = MaterialTheme.typography.headlineSmall
            )

            HorizontalDivider()

            SyncSettingsContent(
                webDAVUrl = webDAVUrl,
                onWebDAVUrlChange = { webDAVUrl = it },
                username = username,
                onUsernameChange = { username = it },
                password = password,
                onPasswordChange = { password = it },
                syncReadingHistory = syncReadingHistory,
                onSyncReadingHistoryChange = { syncReadingHistory = it },
                syncBookshelf = syncBookshelf,
                onSyncBookshelfChange = { syncBookshelf = it },
                syncSettings = syncSettings,
                onSyncSettingsChange = { syncSettings = it },
                autoSyncEnabled = autoSyncEnabled,
                onAutoSyncEnabledChange = { autoSyncEnabled = it },
                autoSyncInterval = autoSyncInterval,
                onAutoSyncIntervalChange = { autoSyncInterval = it },
                syncOnWifiOnly = syncOnWifiOnly,
                onSyncOnWifiOnlyChange = { syncOnWifiOnly = it },
                lastSyncTime = if (lastSyncTime > 0) dateFormat.format(Date(lastSyncTime)) else "从未同步",
                isSyncing = isSyncing,
                syncStatus = syncStatus,
                syncError = syncError,
                onManualSync = {
                    if (webDAVUrl.isBlank()) {
                        syncError = "请先配置 WebDAV 服务器地址"
                        return@SyncSettingsContent
                    }
                    scope.launch {
                        isSyncing = true
                        syncError = null
                        syncStatus = "正在同步..."

                        val config = WebDAVConfig(webDAVUrl, username, password)
                        syncManager?.updateConfig(config)

                        val types = mutableListOf<SyncType>()
                        if (syncReadingHistory) types.add(SyncType.READING_HISTORY)
                        if (syncBookshelf) types.add(SyncType.BOOKSHELF)
                        if (syncSettings) types.add(SyncType.SETTINGS)

                        try {
                            val results = syncManager?.performSync(types) ?: emptyList()
                            val hasError = results.any { !it.success }
                            if (hasError) {
                                val errors = results.filter { !it.success }.mapNotNull { it.errorMessage }
                                syncError = errors.joinToString("; ")
                                syncStatus = "同步失败"
                            } else {
                                syncStatus = "同步完成"
                                lastSyncTime = System.currentTimeMillis()
                            }
                        } catch (e: Exception) {
                            syncError = e.message ?: "未知错误"
                            syncStatus = "同步失败"
                        } finally {
                            isSyncing = false
                        }
                    }
                }
            )
        }
    }
}

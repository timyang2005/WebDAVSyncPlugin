package com.dobao.webdavsync

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.nightfish.lightnovelreader.api.plugin.LightNovelReaderPlugin
import io.nightfish.lightnovelreader.api.plugin.Plugin
import io.nightfish.lightnovelreader.api.ui.components.SettingsSwitchEntry
import io.nightfish.lightnovelreader.api.userdata.UserDataRepositoryApi

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
) : LightNovelReaderPlugin {
    
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
        Log.i("WebDAVSyncPlugin", "WebDAV Sync 插件已加载")
    }
    
    @Composable
    override fun PageContent(paddingValues: PaddingValues) {
        val context = LocalContext.current
        val webDAVUrl by userDataRepositoryApi.stringUserData(PREF_WEBDAV_URL).getFlowWithDefault("").collectAsState("")
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clip(RoundedCornerShape(16.dp)),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val checked by userDataRepositoryApi.booleanUserData(PREF_AUTO_SYNC_ENABLED).getFlowWithDefault(false).collectAsState(false)
            
            SettingsSwitchEntry(
                title = "启用 WebDAV 同步",
                description = "当前服务器: ${if (webDAVUrl.isBlank()) "未配置" else webDAVUrl}",
                checked = checked,
                booleanUserData = userDataRepositoryApi.booleanUserData(PREF_AUTO_SYNC_ENABLED)
            )
        }
    }
}

package com.dobao.webdavsync

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import com.dobao.webdavsync.data.SyncType
import com.dobao.webdavsync.data.WebDAVConfig
import com.dobao.webdavsync.sync.SyncManager
import com.dobao.webdavsync.ui.SyncSettingsContent
import io.nightfish.lightnovelreader.api.plugin.LightNovelReaderPlugin
import io.nightfish.lightnovelreader.api.plugin.Plugin
import io.nightfish.lightnovelreader.api.userdata.UserDataRepositoryApi
import kotlinx.coroutines.launch

@Suppress("unused")
@Plugin(
    version = 1,
    name = "WebDAV Sync",
    versionName = "1.0.0",
    author = "DoBao",
    description = "WebDAV cloud sync for LightNovelReader",
    updateUrl = "https://github.com/timyang2005/WebDAVSyncPlugin",
    apiVersion = 2
)
class WebDAVSyncPlugin(
    private val userDataRepositoryApi: UserDataRepositoryApi
) : LightNovelReaderPlugin() {

    private var syncManager: SyncManager? = null
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)

    companion object {
        const val PREF_WEBDAV_URL = "webdav_url"
        const val PREF_WEBDAV_USERNAME = "webdav_username"
        const val PREF_WEBDAV_PASSWORD = "webdav_password"
        const val PREF_SYNC_RH = "sync_reading_history"
        const val PREF_SYNC_BS = "sync_bookshelf"
        const val PREF_SYNC_ST = "sync_settings"
        const val PREF_AUTO_SYNC = "auto_sync_enabled"
    }

    override fun onLoad() {
        val config = WebDAVConfig(
            url = userDataRepositoryApi.stringUserData(PREF_WEBDAV_URL).get() ?: "",
            username = userDataRepositoryApi.stringUserData(PREF_WEBDAV_USERNAME).get() ?: "",
            password = userDataRepositoryApi.stringUserData(PREF_WEBDAV_PASSWORD).get() ?: ""
        )
        syncManager = SyncManager(config, userDataRepositoryApi)
    }

    @Composable
    override fun PageContent(pv: androidx.compose.foundation.layout.PaddingValues) {
        var url by remember { mutableStateOf("") }
        var user by remember { mutableStateOf("") }
        var pwd by remember { mutableStateOf("") }
        var syncRH by remember { mutableStateOf(true) }
        var syncBS by remember { mutableStateOf(true) }
        var syncST by remember { mutableStateOf(true) }
        var autoSync by remember { mutableStateOf(false) }
        var status by remember { mutableStateOf("") }
        val sc = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            url = userDataRepositoryApi.stringUserData(PREF_WEBDAV_URL).get() ?: ""
            user = userDataRepositoryApi.stringUserData(PREF_WEBDAV_USERNAME).get() ?: ""
            pwd = userDataRepositoryApi.stringUserData(PREF_WEBDAV_PASSWORD).get() ?: ""
            syncRH = userDataRepositoryApi.booleanUserData(PREF_SYNC_RH).get() ?: true
            syncBS = userDataRepositoryApi.booleanUserData(PREF_SYNC_BS).get() ?: true
            syncST = userDataRepositoryApi.booleanUserData(PREF_SYNC_ST).get() ?: true
            autoSync = userDataRepositoryApi.booleanUserData(PREF_AUTO_SYNC).get() ?: false
        }

        LazyColumn(Modifier.fillMaxSize().padding(pv)) {
            item {
                SyncSettingsContent(
                    webDAVUrl = url, username = user, password = pwd,
                    syncReadingHistory = syncRH, syncBookshelf = syncBS, syncSettings = syncST,
                    autoSyncEnabled = autoSync, statusMessage = status,
                    onWebDAVUrlChange = { url = it }, onUsernameChange = { user = it }, onPasswordChange = { pwd = it },
                    onSyncReadingHistoryChange = { syncRH = it }, onSyncBookshelfChange = { syncBS = it },
                    onSyncSettingsChange = { syncST = it }, onAutoSyncChange = { autoSync = it },
                    onSyncClick = {
                        sc.launch {
                            status = "Syncing..."
                            syncManager?.updateConfig(WebDAVConfig(url, user, pwd))
                            val types = mutableListOf<SyncType>()
                            if (syncRH) types.add(SyncType.READING_HISTORY)
                            if (syncBS) types.add(SyncType.BOOKSHELF)
                            if (syncST) types.add(SyncType.SETTINGS)
                            val results = syncManager?.performSync(types) { status = it } ?: emptyList()
                            val failed = results.filter { !it.success }
                            status = if (failed.isEmpty()) "Sync complete" else "Sync failed"
                        }
                    },
                    onSaveConfig = {
                        sc.launch {
                            userDataRepositoryApi.stringUserData(PREF_WEBDAV_URL).set(url)
                            userDataRepositoryApi.stringUserData(PREF_WEBDAV_USERNAME).set(user)
                            userDataRepositoryApi.stringUserData(PREF_WEBDAV_PASSWORD).set(pwd)
                            userDataRepositoryApi.booleanUserData(PREF_SYNC_RH).set(syncRH)
                            userDataRepositoryApi.booleanUserData(PREF_SYNC_BS).set(syncBS)
                            userDataRepositoryApi.booleanUserData(PREF_SYNC_ST).set(syncST)
                            userDataRepositoryApi.booleanUserData(PREF_AUTO_SYNC).set(autoSync)
                            status = "Config saved"
                        }
                    }
                )
            }
        }
    }
}

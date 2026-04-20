package com.dobao.webdavsync

import io.nightfish.lightnovelreader.api.plugin.LightNovelReaderPlugin
import io.nightfish.lightnovelreader.api.plugin.Plugin
import io.nightfish.lightnovelreader.api.userdata.UserDataRepositoryApi

@Suppress("unused")
@Plugin(
    version = BuildConfig.VERSION_CODE,
    name = "WebDAV Sync",
    versionName = BuildConfig.VERSION_NAME,
    author = "DoBao",
    description = "WebDAV cloud sync for LightNovelReader",
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
        const val PREF_AUTO_SYNC = "auto_sync_enabled"
        const val DEFAULT_SYNC_INTERVAL = 30
    }

    override fun onLoad() {
        // Initialization
    }
}

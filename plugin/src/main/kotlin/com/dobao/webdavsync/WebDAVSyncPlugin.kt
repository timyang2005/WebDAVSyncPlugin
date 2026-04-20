package com.dobao.webdavsync

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import io.nightfish.lightnovelreader.api.plugin.LightNovelReaderPlugin
import io.nightfish.lightnovelreader.api.plugin.Plugin
import io.nightfish.lightnovelreader.api.userdata.UserDataRepositoryApi
import io.nightfish.lightnovelreader.api.userdata.UserDataDaoApi
import io.nightfish.lightnovelreader.api.book.BookRepositoryApi
import io.nightfish.lightnovelreader.api.bookshelf.BookshelfRepositoryApi
import io.nightfish.lightnovelreader.api.book.LocalBookDataSourceApi

@Plugin(
    version = 1,
    name = "WebDAV Sync",
    versionName = "1.0.0",
    author = "DoBao",
    description = "WebDAV 云同步插件",
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

    companion object {
        const val PREF_WEBDAV_URL = "webdav_url"
        const val PREF_WEBDAV_USERNAME = "webdav_username"
        const val PREF_WEBDAV_PASSWORD = "webdav_password"
    }

    override fun onLoad() {
        // WebDAV 配置将在首次使用时加载
    }

    @Composable
    override fun PageContent(paddingValues: PaddingValues) {
        Text(
            text = "WebDAV Sync Plugin",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

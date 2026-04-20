package com.dobao.webdavsync.sync

import android.util.Log
import com.dobao.webdavsync.data.SyncData
import com.dobao.webdavsync.data.SyncResult
import com.dobao.webdavsync.data.SyncType
import com.dobao.webdavsync.data.WebDAVConfig
import com.dobao.webdavsync.webdav.WebDAVClient
import io.nightfish.lightnovelreader.api.userdata.UserDataRepositoryApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * 同步管理器 - 协调所有同步操作
 */
class SyncManager(
    private var webDAVConfig: WebDAVConfig,
    private val userDataRepositoryApi: UserDataRepositoryApi
) {
    private val webDAVClient = WebDAVClient(webDAVConfig)
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val deviceId: String = UUID.randomUUID().toString()

    companion object {
        private const val SYNC_FILE_READING_HISTORY = "reading_history.json"
        private const val SYNC_FILE_BOOKSHELF = "bookshelf.json"
        private const val SYNC_FILE_SETTINGS = "settings.json"
        private const val SYNC_FILE_MANIFEST = "sync_manifest.json"
        private const val SYNC_DIR_BASE = "/LightNovelReader"
        private const val SYNC_DIR_READING_HISTORY = "$SYNC_DIR_BASE/reading_history"
        private const val SYNC_DIR_BOOKSHELF = "$SYNC_DIR_BASE/bookshelf"
        private const val SYNC_DIR_SETTINGS = "$SYNC_DIR_BASE/settings"
    }

    fun updateConfig(config: WebDAVConfig) {
        this.webDAVConfig = config
        webDAVClient.updateConfig(config)
    }

    suspend fun performSync(
        syncTypes: List<SyncType>
    ): List<SyncResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SyncResult>()

        if (!webDAVConfig.isValid()) {
            return@withContext listOf(
                SyncResult(false, SyncType.SETTINGS, errorMessage = "WebDAV 配置无效")
            )
        }

        // 确保基础目录存在
        ensureBaseDirectories()

        for (syncType in syncTypes) {
            val result = when (syncType) {
                SyncType.READING_HISTORY -> syncReadingHistory()
                SyncType.BOOKSHELF -> syncBookshelf()
                SyncType.SETTINGS -> syncSettings()
            }
            results.add(result)
        }

        results
    }

    private suspend fun ensureBaseDirectories() = withContext(Dispatchers.IO) {
        listOf(SYNC_DIR_BASE, SYNC_DIR_READING_HISTORY, SYNC_DIR_BOOKSHELF, SYNC_DIR_SETTINGS)
            .forEach { dir ->
                webDAVClient.createDirectory(dir)
            }
    }

    private suspend fun syncReadingHistory(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val remotePath = "$SYNC_DIR_READING_HISTORY/$SYNC_FILE_READING_HISTORY"

            // 下载云端数据
            val remoteData = webDAVClient.downloadFile(remotePath).getOrNull()?.let {
                json.decodeFromString<SyncData>(String(it))
            }

            // 读取本地所有设置数据作为阅读历史（简化实现）
            val localData = SyncData(
                version = 1,
                timestamp = System.currentTimeMillis(),
                deviceId = deviceId,
                data = mapOf("type" to "reading_history_placeholder")
            )

            // 合并数据（以时间戳为准）
            val merged = if (remoteData != null && remoteData.timestamp > localData.timestamp) {
                remoteData
            } else {
                localData
            }

            // 上传
            val jsonData = json.encodeToString(merged)
            val uploadResult = webDAVClient.uploadFile(remotePath, jsonData.toByteArray())

            if (uploadResult.isSuccess) {
                SyncResult(true, SyncType.READING_HISTORY)
            } else {
                SyncResult(false, SyncType.READING_HISTORY, errorMessage = uploadResult.exceptionOrNull()?.message)
            }
        } catch (e: Exception) {
            SyncResult(false, SyncType.READING_HISTORY, errorMessage = e.message)
        }
    }

    private suspend fun syncBookshelf(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val remotePath = "$SYNC_DIR_BOOKSHELF/$SYNC_FILE_BOOKSHELF"

            val remoteData = webDAVClient.downloadFile(remotePath).getOrNull()?.let {
                json.decodeFromString<SyncData>(String(it))
            }

            val localData = SyncData(
                version = 1,
                timestamp = System.currentTimeMillis(),
                deviceId = deviceId,
                data = mapOf("type" to "bookshelf_placeholder")
            )

            val merged = if (remoteData != null && remoteData.timestamp > localData.timestamp) {
                remoteData
            } else {
                localData
            }

            val jsonData = json.encodeToString(merged)
            val uploadResult = webDAVClient.uploadFile(remotePath, jsonData.toByteArray())

            if (uploadResult.isSuccess) {
                SyncResult(true, SyncType.BOOKSHELF)
            } else {
                SyncResult(false, SyncType.BOOKSHELF, errorMessage = uploadResult.exceptionOrNull()?.message)
            }
        } catch (e: Exception) {
            SyncResult(false, SyncType.BOOKSHELF, errorMessage = e.message)
        }
    }

    private suspend fun syncSettings(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val remotePath = "$SYNC_DIR_SETTINGS/$SYNC_FILE_SETTINGS"

            val remoteData = webDAVClient.downloadFile(remotePath).getOrNull()?.let {
                json.decodeFromString<SyncData>(String(it))
            }

            val localData = SyncData(
                version = 1,
                timestamp = System.currentTimeMillis(),
                deviceId = deviceId,
                data = mapOf("type" to "settings_placeholder")
            )

            val merged = if (remoteData != null && remoteData.timestamp > localData.timestamp) {
                remoteData
            } else {
                localData
            }

            val jsonData = json.encodeToString(merged)
            val uploadResult = webDAVClient.uploadFile(remotePath, jsonData.toByteArray())

            if (uploadResult.isSuccess) {
                SyncResult(true, SyncType.SETTINGS)
            } else {
                SyncResult(false, SyncType.SETTINGS, errorMessage = uploadResult.exceptionOrNull()?.message)
            }
        } catch (e: Exception) {
            SyncResult(false, SyncType.SETTINGS, errorMessage = e.message)
        }
    }
}

package com.dobao.webdavsync.sync

import com.dobao.webdavsync.data.*
import com.dobao.webdavsync.webdav.WebDAVClient
import io.nightfish.lightnovelreader.api.book.BookRepositoryApi
import io.nightfish.lightnovelreader.api.bookshelf.BookshelfRepositoryApi
import io.nightfish.lightnovelreader.api.book.LocalBookDataSourceApi
import io.nightfish.lightnovelreader.api.userdata.UserDataDaoApi
import io.nightfish.lightnovelreader.api.userdata.UserDataRepositoryApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/**
 * 同步管理器 - 协调所有同步操作
 */
class SyncManager(
    private var webDAVConfig: WebDAVConfig,
    private val userDataRepositoryApi: UserDataRepositoryApi,
    private val userDataDaoApi: UserDataDaoApi,
    private val bookRepositoryApi: BookRepositoryApi,
    private val bookshelfRepositoryApi: BookshelfRepositoryApi,
    private val localBookDataSourceApi: LocalBookDataSourceApi
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
        
        private const val PREF_LAST_SYNC_TIME = "last_sync_time"
    }

    /**
     * 更新 WebDAV 配置
     */
    fun updateConfig(config: WebDAVConfig) {
        webDAVConfig = config
        webDAVClient.updateConfig(config)
    }
    
    /**
     * 执行同步
     */
    suspend fun performSync(
        syncTypes: List<SyncType>,
        onProgress: (String) -> Unit
    ): List<SyncResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SyncResult>()
        
        if (!webDAVConfig.isValid()) {
            return@withContext listOf(
                SyncResult(false, SyncType.SETTINGS, SyncDirection.UPLOAD, 
                    errorMessage = "WebDAV 配置无效")
            )
        }
        
        // 确保基础目录存在
        onProgress("检查云端目录...")
        ensureBaseDirectories()
        
        for (syncType in syncTypes) {
            onProgress("同步 ${getSyncTypeName(syncType)}...")
            
            val result = when (syncType) {
                SyncType.READING_HISTORY -> syncReadingHistory()
                SyncType.BOOKSHELF -> syncBookshelf()
                SyncType.SETTINGS -> syncSettings()
            }
            results.add(result)
            
            onProgress("${getSyncTypeName(syncType)} 同步完成")
        }
        
        // 更新同步清单
        updateSyncManifest()
        
        // 更新最后同步时间
        userDataRepositoryApi.stringUserData(PREF_LAST_SYNC_TIME).set(System.currentTimeMillis().toString())
        
        results
    }

    /**
     * 确保基础目录存在
     */
    private suspend fun ensureBaseDirectories() = withContext(Dispatchers.IO) {
        listOf(SYNC_DIR_BASE, SYNC_DIR_READING_HISTORY, SYNC_DIR_BOOKSHELF, SYNC_DIR_SETTINGS)
            .forEach { dir ->
                webDAVClient.createDirectory(dir)
            }
    }
    
    /**
     * 同步阅读历史
     */
    private suspend fun syncReadingHistory(): SyncResult = withContext(Dispatchers.IO) {
        try {
            // 获取本地阅读数据
            val localReadingData = localBookDataSourceApi.getAllUserReadingData()
            
            // 转换为同步格式
            val historyItems = localReadingData.map { data ->
                ReadingHistoryItem(
                    bookId = data.id,
                    lastReadTime = data.lastReadTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    totalReadTime = data.totalReadTime,
                    readingProgress = data.readingProgress,
                    lastReadChapterId = data.lastReadChapterId,
                    lastReadChapterTitle = data.lastReadChapterTitle,
                    currentChapterProgressMap = data.currentChapterReadingProgressMap,
                    maxChapterProgressMap = data.maxChapterReadingProgressMap
                )
            }
            
            val localContainer = SyncDataContainer(
                version = 1,
                timestamp = System.currentTimeMillis(),
                deviceId = deviceId,
                readingHistory = historyItems
            )
            
            // 下载云端数据
            val remotePath = "$SYNC_DIR_READING_HISTORY/$SYNC_FILE_READING_HISTORY"
            val remoteData = if (webDAVClient.exists(remotePath)) {
                val downloadResult = webDAVClient.downloadFile(remotePath)
                downloadResult.getOrNull()?.let { bytes ->
                    json.decodeFromString<SyncDataContainer>(String(bytes))
                }
            } else null
            
            // 合并数据（本地优先）
            val mergedData = mergeReadingHistory(localContainer, remoteData)
            
            // 上传合并后的数据
            val jsonData = json.encodeToString(mergedData)
            webDAVClient.uploadFile(remotePath, jsonData.toByteArray())
            
            SyncResult(true, SyncType.READING_HISTORY, SyncDirection.BOTH)
        } catch (e: Exception) {
            SyncResult(false, SyncType.READING_HISTORY, SyncDirection.UPLOAD, 
                errorMessage = e.message)
        }
    }

    /**
     * 同步书架
     */
    private suspend fun syncBookshelf(): SyncResult = withContext(Dispatchers.IO) {
        try {
            // 获取本地书架
            val localShelf = bookshelfRepositoryApi.getBookshelf().first()
            
            val localItems = localShelf.map { shelf ->
                BookshelfItem(
                    bookId = shelf.bookId,
                    bookTitle = shelf.bookTitle,
                    author = shelf.author,
                    coverUrl = shelf.coverUrl,
                    lastChapterId = shelf.lastChapterId,
                    lastChapterTitle = shelf.lastChapterTitle,
                    addedTime = shelf.addedTime,
                    lastReadTime = shelf.lastReadTime,
                    dataSourceId = shelf.dataSourceId,
                    dataSourceName = shelf.dataSourceName,
                    isSubscribed = shelf.isSubscribed,
                    customOrder = shelf.customOrder
                )
            }
            
            val localContainer = SyncDataContainer(
                version = 1,
                timestamp = System.currentTimeMillis(),
                deviceId = deviceId,
                bookshelf = localItems
            )
            
            // 下载云端数据
            val remotePath = "$SYNC_DIR_BOOKSHELF/$SYNC_FILE_BOOKSHELF"
            val remoteData = if (webDAVClient.exists(remotePath)) {
                val downloadResult = webDAVClient.downloadFile(remotePath)
                downloadResult.getOrNull()?.let { bytes ->
                    json.decodeFromString<SyncDataContainer>(String(bytes))
                }
            } else null
            
            // 合并数据
            val mergedData = mergeBookshelf(localContainer, remoteData)
            
            // 上传合并后的数据
            val jsonData = json.encodeToString(mergedData)
            webDAVClient.uploadFile(remotePath, jsonData.toByteArray())
            
            SyncResult(true, SyncType.BOOKSHELF, SyncDirection.BOTH)
        } catch (e: Exception) {
            SyncResult(false, SyncType.BOOKSHELF, SyncDirection.UPLOAD, 
                errorMessage = e.message)
        }
    }

    /**
     * 同步设置
     */
    private suspend fun syncSettings(): SyncResult = withContext(Dispatchers.IO) {
        try {
            // 获取本地阅读数据中的设置信息
            val settingsMap = mutableMapOf<String, String>()
            
            val allReadingData = localBookDataSourceApi.getAllUserReadingData()
            for (data in allReadingData) {
                settingsMap["reading_${data.id}_chapter"] = data.lastReadChapterId
                settingsMap["reading_${data.id}_progress"] = data.readingProgress.toString()
                settingsMap["reading_${data.id}_totalTime"] = data.totalReadTime.toString()
            }
            
            val localContainer = SyncDataContainer(
                version = 1,
                timestamp = System.currentTimeMillis(),
                deviceId = deviceId,
                settings = settingsMap
            )
            
            // 下载云端数据
            val remotePath = "$SYNC_DIR_SETTINGS/$SYNC_FILE_SETTINGS"
            val remoteData = if (webDAVClient.exists(remotePath)) {
                val downloadResult = webDAVClient.downloadFile(remotePath)
                downloadResult.getOrNull()?.let { bytes ->
                    json.decodeFromString<SyncDataContainer>(String(bytes))
                }
            } else null
            
            // 合并设置
            val mergedData = mergeSettings(localContainer, remoteData)
            
            // 应用远程设置到本地
            mergedData.settings?.forEach { (key, value) ->
                userDataRepositoryApi.stringUserData(key).set(value)
            }
            
            // 上传合并后的数据
            val jsonData = json.encodeToString(mergedData)
            webDAVClient.uploadFile(remotePath, jsonData.toByteArray())
            
            SyncResult(true, SyncType.SETTINGS, SyncDirection.BOTH)
        } catch (e: Exception) {
            SyncResult(false, SyncType.SETTINGS, SyncDirection.UPLOAD, 
                errorMessage = e.message)
        }
    }

    /**
     * 合并阅读历史
     */
    private fun mergeReadingHistory(local: SyncDataContainer?, remote: SyncDataContainer?): SyncDataContainer {
        if (local == null && remote == null) {
            return SyncDataContainer(deviceId = deviceId)
        }
        if (local == null) return remote!!
        if (remote == null) return local
        
        // 以时间戳较新的为准
        return if (local.timestamp > remote.timestamp) local else remote
    }

    /**
     * 合并书架
     */
    private fun mergeBookshelf(local: SyncDataContainer?, remote: SyncDataContainer?): SyncDataContainer {
        if (local == null && remote == null) {
            return SyncDataContainer(deviceId = deviceId)
        }
        if (local == null) return remote!!
        if (remote == null) return local
        
        // 合并书架项
        val mergedBooks = (local.bookshelf ?: emptyList()).associateBy { it.bookId }.toMutableMap()
        remote.bookshelf?.forEach { item ->
            if (!mergedBooks.containsKey(item.bookId) ||
                (item.lastReadTime ?: 0) > (mergedBooks[item.bookId]?.lastReadTime ?: 0)) {
                mergedBooks[item.bookId] = item
            }
        }
        
        return local.copy(bookshelf = mergedBooks.values.toList())
    }

    /**
     * 合并设置
     */
    private fun mergeSettings(local: SyncDataContainer?, remote: SyncDataContainer?): SyncDataContainer {
        if (local == null && remote == null) {
            return SyncDataContainer(deviceId = deviceId)
        }
        if (local == null) return remote!!
        if (remote == null) return local
        
        // 合并设置，远程优先
        val mergedSettings = (remote.settings ?: emptyMap()).toMutableMap()
        local.settings?.forEach { (key, value) ->
            if (!mergedSettings.containsKey(key)) {
                mergedSettings[key] = value
            }
        }
        
        return local.copy(settings = mergedSettings)
    }

    /**
     * 更新同步清单
     */
    private suspend fun updateSyncManifest() = withContext(Dispatchers.IO) {
        val manifest = SyncManifest(
            lastSyncTime = System.currentTimeMillis(),
            deviceId = deviceId,
            syncedTypes = listOf("reading_history", "bookshelf", "settings")
        )
        
        val manifestPath = "$SYNC_DIR_BASE/$SYNC_FILE_MANIFEST"
        val jsonData = json.encodeToString(manifest)
        webDAVClient.uploadFile(manifestPath, jsonData.toByteArray())
    }

    private fun getSyncTypeName(type: SyncType): String = when (type) {
        SyncType.READING_HISTORY -> "阅读历史"
        SyncType.BOOKSHELF -> "书架"
        SyncType.SETTINGS -> "设置"
    }
}

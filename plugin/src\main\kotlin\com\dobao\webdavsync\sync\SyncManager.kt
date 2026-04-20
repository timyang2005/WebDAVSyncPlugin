package com.dobao.webdavsync.sync

import android.content.Context
import android.provider.Settings
import com.dobao.webdavsync.data.*
import com.dobao.webdavsync.webdav.WebDAVClient
import io.nightfish.lightnovelreader.api.book.BookRepositoryApi
import io.nightfish.lightnovelreader.api.bookshelf.BookshelfRepositoryApi
import io.nightfish.lightnovelreader.api.LocalBookDataSourceApi
import io.nightfish.lightnovelreader.api.userdata.UserDataDaoApi
import io.nightfish.lightnovelreader.api.userdata.UserDataRepositoryApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
        
        results
    }
    
    /**
     * 同步阅读历史
     */
    private suspend fun syncReadingHistory(): SyncResult = withContext(Dispatchers.IO) {
        try {
            onProgress("读取本地阅读历史...")
            val localHistory = loadLocalReadingHistory()
            
            onProgress("检查云端阅读历史...")
            val remotePath = "$SYNC_DIR_READING_HISTORY/$SYNC_FILE_READING_HISTORY"
            
            // 下载云端数据
            val remoteData = if (webDAVClient.exists(remotePath)) {
                val downloadResult = webDAVClient.downloadFile(remotePath)
                downloadResult.getOrNull()?.let { bytes ->
                    json.decodeFromString<SyncDataContainer>(String(bytes))
                }
            } else {
                null
            }
            
            // 合并数据（本地优先，保留最新的阅读记录）
            val mergedData = mergeReadingHistory(localHistory, remoteData)
            
            // 上传合并后的数据
            val jsonData = json.encodeToString(mergedData)
            val uploadResult = webDAVClient.uploadFile(remotePath, jsonData.toByteArray())
            
            if (uploadResult.isSuccess) {
                SyncResult(true, SyncType.READING_HISTORY, SyncDirection.BIDIRECTIONAL, 
                    itemCount = mergedData.readingHistory.size)
            } else {
                SyncResult(false, SyncType.READING_HISTORY, SyncDirection.UPLOAD,
                    errorMessage = uploadResult.exceptionOrNull()?.message)
            }
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
            onProgress("读取本地书架...")
            val localBookshelf = loadLocalBookshelf()
            
            onProgress("检查云端书架...")
            val remotePath = "$SYNC_DIR_BOOKSHELF/$SYNC_FILE_BOOKSHELF"
            
            // 下载云端数据
            val remoteData = if (webDAVClient.exists(remotePath)) {
                val downloadResult = webDAVClient.downloadFile(remotePath)
                downloadResult.getOrNull()?.let { bytes ->
                    json.decodeFromString<SyncDataContainer>(String(bytes))
                }
            } else {
                null
            }
            
            // 合并数据
            val mergedData = mergeBookshelf(localBookshelf, remoteData)
            
            // 上传合并后的数据
            val jsonData = json.encodeToString(mergedData)
            val uploadResult = webDAVClient.uploadFile(remotePath, jsonData.toByteArray())
            
            if (uploadResult.isSuccess) {
                SyncResult(true, SyncType.BOOKSHELF, SyncDirection.BIDIRECTIONAL,
                    itemCount = mergedData.bookshelf.size)
            } else {
                SyncResult(false, SyncType.BOOKSHELF, SyncDirection.UPLOAD,
                    errorMessage = uploadResult.exceptionOrNull()?.message)
            }
        } catch (e: Exception) {
            SyncResult(false, SyncType.BOOKSHELF, SyncDirection.UPLOAD,
                errorMessage = e.message)
        }
    }
    
    /**
     * 同步软件设置
     */
    private suspend fun syncSettings(): SyncResult = withContext(Dispatchers.IO) {
        try {
            onProgress("读取本地设置...")
            val localSettings = loadLocalSettings()
            
            onProgress("检查云端设置...")
            val remotePath = "$SYNC_DIR_SETTINGS/$SYNC_FILE_SETTINGS"
            
            // 下载云端数据
            val remoteData = if (webDAVClient.exists(remotePath)) {
                val downloadResult = webDAVClient.downloadFile(remotePath)
                downloadResult.getOrNull()?.let { bytes ->
                    json.decodeFromString<SyncDataContainer>(String(bytes))
                }
            } else {
                null
            }
            
            // 合并设置（本地优先，除非云端是更新的）
            val mergedSettings = mergeSettings(localSettings, remoteData?.settings ?: emptyMap())
            
            // 保存合并后的设置到本地
            saveLocalSettings(mergedSettings)
            
            // 上传设置到云端
            val container = SyncDataContainer(
                version = 1,
                timestamp = System.currentTimeMillis(),
                deviceId = deviceId,
                settings = mergedSettings
            )
            val jsonData = json.encodeToString(container)
            val uploadResult = webDAVClient.uploadFile(remotePath, jsonData.toByteArray())
            
            if (uploadResult.isSuccess) {
                SyncResult(true, SyncType.SETTINGS, SyncDirection.BIDIRECTIONAL,
                    itemCount = mergedSettings.size)
            } else {
                SyncResult(false, SyncType.SETTINGS, SyncDirection.UPLOAD,
                    errorMessage = uploadResult.exceptionOrNull()?.message)
            }
        } catch (e: Exception) {
            SyncResult(false, SyncType.SETTINGS, SyncDirection.UPLOAD,
                errorMessage = e.message)
        }
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
     * 加载本地阅读历史
     */
    private suspend fun loadLocalReadingHistory(): SyncDataContainer = withContext(Dispatchers.IO) {
        try {
            // 使用 UserDataDaoApi 获取阅读历史
            // 这里需要根据实际的 API 来实现
            val historyData = userDataDaoApi.getAllUserData()
                .first()
                .filter { it.key.startsWith("reading_history_") }
                .mapNotNull { (key, value) ->
                    try {
                        json.decodeFromString<ReadingHistoryItem>(value)
                    } catch (e: Exception) {
                        null
                    }
                }
            
            SyncDataContainer(
                version = 1,
                timestamp = System.currentTimeMillis(),
                deviceId = deviceId,
                readingHistory = historyData
            )
        } catch (e: Exception) {
            SyncDataContainer(deviceId = deviceId)
        }
    }
    
    /**
     * 加载本地书架
     */
    private suspend fun loadLocalBookshelf(): SyncDataContainer = withContext(Dispatchers.IO) {
        try {
            // 获取书架数据
            val bookshelfItems = bookshelfRepositoryApi.getBookshelf().first()
                .map { shelf ->
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
            
            SyncDataContainer(
                version = 1,
                timestamp = System.currentTimeMillis(),
                deviceId = deviceId,
                bookshelf = bookshelfItems
            )
        } catch (e: Exception) {
            SyncDataContainer(deviceId = deviceId)
        }
    }
    
    /**
     * 加载本地设置
     */
    private suspend fun loadLocalSettings(): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            userDataDaoApi.getAllUserData()
                .first()
                .filter { !it.key.startsWith("webdav_") && !it.key.startsWith("sync_") }
                .associate { it.key to it.value }
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    /**
     * 保存设置到本地
     */
    private suspend fun saveLocalSettings(settings: Map<String, String>) = withContext(Dispatchers.IO) {
        settings.forEach { (key, value) ->
            userDataRepositoryApi.stringUserData(key).set(value)
        }
    }
    
    /**
     * 合并阅读历史
     */
    private fun mergeReadingHistory(
        local: SyncDataContainer,
        remote: SyncDataContainer?
    ): SyncDataContainer {
        if (remote == null) return local
        
        // 以最后阅读时间为准，保留最新的记录
        val mergedMap = mutableMapOf<String, ReadingHistoryItem>()
        
        // 先添加远程数据
        remote.readingHistory.forEach { item ->
            mergedMap[item.bookId] = item
        }
        
        // 本地数据覆盖远程数据（如果本地更新）
        local.readingHistory.forEach { localItem ->
            val existing = mergedMap[localItem.bookId]
            if (existing == null || localItem.lastReadTime > existing.lastReadTime) {
                mergedMap[localItem.bookId] = localItem
            }
        }
        
        return SyncDataContainer(
            version = 1,
            timestamp = System.currentTimeMillis(),
            deviceId = deviceId,
            readingHistory = mergedMap.values.toList(),
            bookshelf = local.bookshelf,
            settings = local.settings
        )
    }
    
    /**
     * 合并书架
     */
    private fun mergeBookshelf(
        local: SyncDataContainer,
        remote: SyncDataContainer?
    ): SyncDataContainer {
        if (remote == null) return local
        
        // 以添加时间为准，合并书架
        val mergedMap = mutableMapOf<String, BookshelfItem>()
        
        remote.bookshelf.forEach { item ->
            mergedMap[item.bookId] = item
        }
        
        local.bookshelf.forEach { localItem ->
            val existing = mergedMap[localItem.bookId]
            if (existing == null || localItem.addedTime > existing.addedTime) {
                mergedMap[localItem.bookId] = localItem
            }
        }
        
        return SyncDataContainer(
            version = 1,
            timestamp = System.currentTimeMillis(),
            deviceId = deviceId,
            readingHistory = local.readingHistory,
            bookshelf = mergedMap.values.toList(),
            settings = local.settings
        )
    }
    
    /**
     * 合并设置
     */
    private fun mergeSettings(
        local: Map<String, String>,
        remote: Map<String, String>
    ): Map<String, String> {
        // 本地设置优先，但如果远程有本地没有的设置，则采用远程设置
        val merged = remote.toMutableMap()
        merged.putAll(local)
        return merged
    }
    
    /**
     * 更新同步清单
     */
    private suspend fun updateSyncManifest() = withContext(Dispatchers.IO) {
        val manifest = mapOf(
            "lastSyncTime" to System.currentTimeMillis().toString(),
            "deviceId" to deviceId,
            "version" to "1"
        )
        val jsonData = json.encodeToString(manifest)
        webDAVClient.uploadFile("$SYNC_DIR_BASE/$SYNC_FILE_MANIFEST", jsonData.toByteArray())
    }
    
    /**
     * 获取同步类型名称
     */
    private fun getSyncTypeName(type: SyncType): String {
        return when (type) {
            SyncType.READING_HISTORY -> "阅读记录"
            SyncType.BOOKSHELF -> "书架"
            SyncType.SETTINGS -> "软件设置"
        }
    }
}

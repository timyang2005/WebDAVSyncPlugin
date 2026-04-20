package com.dobao.webdavsync.sync

import android.util.Log
import com.dobao.webdavsync.data.*
import com.dobao.webdavsync.webdav.WebDAVClient
import io.nightfish.lightnovelreader.api.userdata.UserDataRepositoryApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class SyncManager(
    private var webDAVConfig: WebDAVConfig,
    private val userDataRepositoryApi: UserDataRepositoryApi
) {
    private val client = WebDAVClient(webDAVConfig)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }
    private val deviceId = UUID.randomUUID().toString()

    companion object {
        private const val BASE = "/LightNovelReader"
        private const val DIR_RH = BASE + "/reading_history"
        private const val DIR_BS = BASE + "/bookshelf"
        private const val DIR_ST = BASE + "/settings"
        private const val FILE_RH = "reading_history.json"
        private const val FILE_BS = "bookshelf.json"
        private const val FILE_ST = "settings.json"
    }

    fun updateConfig(config: WebDAVConfig) {
        this.webDAVConfig = config
        client.updateConfig(config)
    }

    suspend fun performSync(types: List<SyncType>, onProgress: (String) -> Unit = {}): List<SyncResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SyncResult>()
        if (!webDAVConfig.isValid()) return@withContext listOf(SyncResult(false, SyncType.SETTINGS, errorMessage = "Invalid WebDAV config"))
        onProgress("Checking cloud directories...")
        listOf(BASE, DIR_RH, DIR_BS, DIR_ST).forEach { client.createDirectory(it) }
        for (type in types) {
            onProgress("Syncing " + type.name + "...")
            val result = when (type) {
                SyncType.READING_HISTORY -> syncReadingHistory()
                SyncType.BOOKSHELF -> syncBookshelf()
                SyncType.SETTINGS -> syncSettings()
            }
            results.add(result)
        }
        results
    }

    private suspend fun syncReadingHistory(): SyncResult {
        return try {
            val remotePath = DIR_RH + "/" + FILE_RH
            val remote = client.downloadFile(remotePath).getOrNull()?.let { json.decodeFromString<SyncDataContainer>(String(it)) }
            val local = SyncDataContainer(version = 1, timestamp = System.currentTimeMillis(), deviceId = deviceId)
            val merged = if (remote != null && remote.timestamp > local.timestamp) remote else local
            val ok = client.uploadFile(remotePath, json.encodeToString(merged).toByteArray()).isSuccess
            SyncResult(ok, SyncType.READING_HISTORY)
        } catch (e: Exception) { Log.e("SyncManager", "RH sync failed", e); SyncResult(false, SyncType.READING_HISTORY, errorMessage = e.message) }
    }

    private suspend fun syncBookshelf(): SyncResult {
        return try {
            val remotePath = DIR_BS + "/" + FILE_BS
            val remote = client.downloadFile(remotePath).getOrNull()?.let { json.decodeFromString<SyncDataContainer>(String(it)) }
            val local = SyncDataContainer(version = 1, timestamp = System.currentTimeMillis(), deviceId = deviceId)
            val merged = if (remote != null && remote.timestamp > local.timestamp) remote else local
            val ok = client.uploadFile(remotePath, json.encodeToString(merged).toByteArray()).isSuccess
            SyncResult(ok, SyncType.BOOKSHELF)
        } catch (e: Exception) { Log.e("SyncManager", "BS sync failed", e); SyncResult(false, SyncType.BOOKSHELF, errorMessage = e.message) }
    }

    private suspend fun syncSettings(): SyncResult {
        return try {
            val remotePath = DIR_ST + "/" + FILE_ST
            val remote = client.downloadFile(remotePath).getOrNull()?.let { json.decodeFromString<SyncDataContainer>(String(it)) }
            val local = SyncDataContainer(version = 1, timestamp = System.currentTimeMillis(), deviceId = deviceId)
            val merged = if (remote != null && remote.timestamp > local.timestamp) remote else local
            val ok = client.uploadFile(remotePath, json.encodeToString(merged).toByteArray()).isSuccess
            SyncResult(ok, SyncType.SETTINGS)
        } catch (e: Exception) { Log.e("SyncManager", "ST sync failed", e); SyncResult(false, SyncType.SETTINGS, errorMessage = e.message) }
    }
}

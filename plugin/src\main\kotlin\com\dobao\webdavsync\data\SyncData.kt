package com.dobao.webdavsync.data

import kotlinx.serialization.Serializable

/**
 * 同步数据类型
 */
enum class SyncType {
    READING_HISTORY,  // 阅读记录
    BOOKSHELF,        // 书架
    SETTINGS          // 软件设置
}

/**
 * 同步状态
 */
enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    FAILED
}

/**
 * 同步方向
 */
enum class SyncDirection {
    UPLOAD,   // 上传到云端
    DOWNLOAD, // 从云端下载
    BIDIRECTIONAL // 双向同步
}

/**
 * 同步数据容器 - 用于序列化和反序列化
 */
@Serializable
data class SyncDataContainer(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val readingHistory: List<ReadingHistoryItem> = emptyList(),
    val bookshelf: List<BookshelfItem> = emptyList(),
    val settings: Map<String, String> = emptyMap()
)

/**
 * 阅读历史记录项
 */
@Serializable
data class ReadingHistoryItem(
    val bookId: String,
    val bookTitle: String,
    val chapterId: String,
    val chapterTitle: String,
    val progress: Float,           // 阅读进度 0.0 - 1.0
    val scrollPosition: Long,      // 滚动位置
    val lastReadTime: Long,        // 最后阅读时间戳
    val readDuration: Long,        // 累计阅读时长(毫秒)
    val dataSourceId: Int,         // 数据源ID
    val dataSourceName: String     // 数据源名称
)

/**
 * 书架项
 */
@Serializable
data class BookshelfItem(
    val bookId: String,
    val bookTitle: String,
    val author: String,
    val coverUrl: String,
    val lastChapterId: String?,
    val lastChapterTitle: String?,
    val addedTime: Long,
    val lastReadTime: Long?,
    val dataSourceId: Int,
    val dataSourceName: String,
    val isSubscribed: Boolean,     // 是否订阅更新
    val customOrder: Int           // 自定义排序顺序
)

/**
 * 同步结果
 */
data class SyncResult(
    val success: Boolean,
    val syncType: SyncType,
    val direction: SyncDirection,
    val itemCount: Int = 0,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

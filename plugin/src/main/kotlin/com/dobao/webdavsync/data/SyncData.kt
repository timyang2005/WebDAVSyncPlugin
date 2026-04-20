package com.dobao.webdavsync.data

import kotlinx.serialization.Serializable

enum class SyncType { READING_HISTORY, BOOKSHELF, SETTINGS }

enum class SyncDirection { UPLOAD, DOWNLOAD, BIDIRECTIONAL }

@Serializable
data class WebDAVConfig(
    val url: String = "",
    val username: String = "",
    val password: String = ""
) {
    fun isValid() = url.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))
}

@Serializable
data class SyncData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val data: Map<String, String> = emptyMap()
)

data class SyncResult(
    val success: Boolean,
    val syncType: SyncType,
    val direction: SyncDirection = SyncDirection.BIDIRECTIONAL,
    val errorMessage: String? = null
)

@Serializable
data class SyncDataContainer(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val readingHistory: List<ReadingHistoryItem> = emptyList(),
    val bookshelf: List<BookshelfItem> = emptyList(),
    val settings: Map<String, String> = emptyMap()
)

@Serializable
data class ReadingHistoryItem(
    val bookId: String,
    val lastReadTime: Long = 0,
    val readingPosition: Long = 0,
    val chapterId: String = "",
    val chapterTitle: String = ""
)

@Serializable
data class BookshelfItem(
    val bookId: String,
    val bookTitle: String,
    val author: String = "",
    val coverUrl: String = "",
    val lastChapterId: String = "",
    val lastChapterTitle: String = "",
    val addedTime: Long = 0,
    val lastReadTime: Long = 0
)

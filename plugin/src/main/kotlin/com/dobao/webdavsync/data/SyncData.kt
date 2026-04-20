package com.dobao.webdavsync.data

import kotlinx.serialization.Serializable

@Serializable
data class WebDAVConfig(
    val url: String = "",
    val username: String = "",
    val password: String = ""
) {
    fun isValid(): Boolean = url.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))
}

@Serializable
data class SyncData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val data: Map<String, String> = emptyMap()
)

@Serializable
data class SyncResult(
    val success: Boolean,
    val syncType: SyncType,
    val errorMessage: String? = null,
    val itemCount: Int = 0
)

enum class SyncType {
    READING_HISTORY,
    BOOKSHELF,
    SETTINGS
}

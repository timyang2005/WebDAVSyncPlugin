package com.dobao.webdavsync.data

/**
 * WebDAV 配置
 */
data class WebDAVConfig(
    val url: String,
    val username: String,
    val password: String,
    val basePath: String = "/LightNovelReader",
    val timeout: Long = 30000L,  // 超时时间(毫秒)
    val verifySsl: Boolean = true // 是否验证 SSL 证书
) {
    /**
     * 检查配置是否有效
     */
    fun isValid(): Boolean {
        return url.isNotBlank() && 
               (username.isBlank() || password.isNotBlank())
    }
    
    /**
     * 获取完整的远程路径
     */
    fun getRemotePath(path: String): String {
        return "$basePath/$path".replace("//", "/")
    }
}

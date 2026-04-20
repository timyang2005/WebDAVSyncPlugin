package com.dobao.webdavsync.webdav

import com.dobao.webdavsync.data.WebDAVConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * WebDAV 客户端 - 处理所有 WebDAV 协议操作
 */
class WebDAVClient(private var config: WebDAVConfig) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private val PROPFIND_REQUEST_BODY = """<?xml version="1.0" encoding="utf-8"?>
<propfind xmlns="DAV:">
  <prop>
    <resourcetype/>
    <getcontentlength/>
    <getlastmodified/>
    <getetag/>
    <displayname/>
  </prop>
</propfind>""".trimIndent()

        private val XML_MEDIA_TYPE = "application/xml; charset=utf-8".toMediaType()
    }

    fun updateConfig(config: WebDAVConfig) {
        this.config = config
    }

    private fun buildRequest(url: String, method: String = "GET", body: okhttp3.RequestBody? = null): Request {
        val builder = Request.Builder()
            .url(normalizeUrl(config.url.trimEnd('/') + "/" + url.trimStart('/')))

        if (config.username.isNotBlank()) {
            builder.addHeader("Authorization", Credentials.basic(config.username, config.password))
        }

        return builder
            .method(method, body)
            .build()
    }

    private fun normalizeUrl(url: String): String {
        return url.replace("\", "/")
    }

    /**
     * 检查文件或目录是否存在
     */
    suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(buildRequest(path, "HEAD")).execute()
            response.close()
            response.code in 200..299 || response.code == 404
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 创建目录 (MKCOL)
     */
    suspend fun createDirectory(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 确保路径不以 / 开头
            val cleanPath = path.trimStart('/')
            val response = client.newCall(buildRequest(cleanPath, "MKCOL")).execute()
            val code = response.code
            response.close()
            code in 200..299 || code == 405 // 405 = 目录已存在
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 上传文件 (PUT)
     */
    suspend fun uploadFile(path: String, content: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanPath = path.trimStart('/')
            val request = buildRequest(cleanPath, "PUT", content.toRequestBody(null))
            val response = client.newCall(request).execute()
            val code = response.code
            response.close()
            if (code in 200..299 || code == 201) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Upload failed: HTTP $code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 下载文件 (GET)
     */
    suspend fun downloadFile(path: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val cleanPath = path.trimStart('/')
            val response = client.newCall(buildRequest(cleanPath)).execute()
            val code = response.code
            if (code in 200..299) {
                Result.success(response.body?.bytes() ?: ByteArray(0))
            } else {
                response.close()
                Result.failure(IOException("Download failed: HTTP $code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除文件或目录 (DELETE)
     */
    suspend fun delete(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanPath = path.trimStart('/')
            val response = client.newCall(buildRequest(cleanPath, "DELETE")).execute()
            response.close()
            response.code in 200..299
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 列出目录内容 (PROPFIND)
     */
    suspend fun listDirectory(path: String): Result<List<WebDAVItem>> = withContext(Dispatchers.IO) {
        try {
            val cleanPath = path.trimStart('/')
            val requestBody = PROPFIND_REQUEST_BODY.toRequestBody(XML_MEDIA_TYPE)
            val request = buildRequest(cleanPath, "PROPFIND", requestBody)
                .newBuilder()
                .addHeader("Depth", "1")
                .build()
            val response = client.newCall(request).execute()
            val code = response.code
            if (code in 207) { // Multi-Status
                val body = response.body?.string() ?: ""
                response.close()
                Result.success(parseWebDAVResponse(body))
            } else {
                response.close()
                Result.failure(IOException("PROPFIND failed: HTTP $code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseWebDAVResponse(xml: String): List<WebDAVItem> {
        val items = mutableListOf<WebDAVItem>()
        try {
            // 简单解析 href 和 displayname
            val hrefRegex = "<D:href>([^<]+)</D:href>".toRegex()
            val displayNameRegex = "<D:displayname>([^<]*)</D:displayname>".toRegex()
            val resourceTypeRegex = "<D:resourcetype>(<D:collection/>)?</D:resourcetype>".toRegex()

            val hrefs = hrefRegex.findAll(xml).map { it.groupValues[1] }.toList()
            val displayNames = displayNameRegex.findAll(xml).map { it.groupValues[1] }.toList()
            val resourceTypes = resourceTypeRegex.findAll(xml).map { it.groupValues[1] }.toList()

            for (i in hrefs.indices) {
                val href = java.net.URLDecoder.decode(hrefs[i], "UTF-8")
                    .replace("/$", "")
                    .substringAfterLast("/")
                val name = if (i < displayNames.size) displayNames[i] else href
                val isDirectory = if (i < resourceTypes.size) resourceTypes[i].isNotEmpty() else false
                items.add(WebDAVItem(name = name.ifBlank { href }, path = href, isDirectory = isDirectory))
            }
        } catch (e: Exception) {
            // 解析失败
        }
        return items
    }
}

data class WebDAVItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModified: String = ""
)

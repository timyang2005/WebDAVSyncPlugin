package com.dobao.webdavsync.webdav

import com.dobao.webdavsync.data.WebDAVConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Element
import org.w3c.dom.Document

/**
 * WebDAV 客户端
 * 支持基本的 WebDAV 操作：PROPFIND, MKCOL, PUT, GET, DELETE
 */
class WebDAVClient(private var config: WebDAVConfig) {
    
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(config.timeout, TimeUnit.MILLISECONDS)
            .readTimeout(config.timeout, TimeUnit.MILLISECONDS)
            .writeTimeout(config.timeout, TimeUnit.MILLISECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val credentials = Credentials.basic(config.username, config.password)
                val request = original.newBuilder()
                    .header("Authorization", credentials)
                    .build()
                chain.proceed(request)
            }
            .build()
    }
    
    /**
     * 更新配置
     */
    fun updateConfig(newConfig: WebDAVConfig) {
        config = newConfig
    }
    
    /**
     * 测试连接
     */
    suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = propfind(config.url, depth = 0)
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 创建目录（MKCOL）
     */
    suspend fun createDirectory(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = normalizeUrl("${config.url}$path")
            val request = Request.Builder()
                .url(url)
                .method("MKCOL", null)
                .build()
            
            val response = client.newCall(request).execute()
            // 201 Created 或 405 Method Not Allowed (目录已存在) 都视为成功
            Result.success(response.code == 201 || response.code == 405)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 确保目录存在
     */
    suspend fun ensureDirectory(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // 尝试创建目录
            val createResult = createDirectory(path)
            if (createResult.getOrNull() == true) {
                return@withContext Result.success(true)
            }
            
            // 检查目录是否已存在
            val propfindResult = propfind("${config.url}$path", depth = 0)
            Result.success(propfindResult.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 上传文件（PUT）
     */
    suspend fun uploadFile(remotePath: String, content: ByteArray): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = normalizeUrl("${config.url}$remotePath")
            val request = Request.Builder()
                .url(url)
                .put(content.toRequestBody("application/octet-stream".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful || response.code == 201 || response.code == 204)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 下载文件（GET）
     */
    suspend fun downloadFile(remotePath: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val url = normalizeUrl("${config.url}$remotePath")
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(response.body?.bytes() ?: ByteArray(0))
            } else {
                Result.failure(IOException("Download failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 删除文件（DELETE）
     */
    suspend fun deleteFile(remotePath: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = normalizeUrl("${config.url}$remotePath")
            val request = Request.Builder()
                .url(url)
                .delete()
                .build()
            
            val response = client.newCall(request).execute()
            // 200, 204, 404 都视为成功（404表示文件本来就不存在）
            Result.success(response.isSuccessful || response.code == 204 || response.code == 404)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 列出目录内容（PROPFIND）
     */
    suspend fun listDirectory(path: String, depth: Int = 1): Result<List<WebDAVItem>> = withContext(Dispatchers.IO) {
        try {
            val response = propfind("${config.url}$path", depth)
            
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val items = parsePropfindResponse(body)
                Result.success(items)
            } else {
                Result.failure(IOException("PROPFIND failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 检查文件是否存在
     */
    suspend fun exists(remotePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = propfind("${config.url}$remotePath", depth = 0)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 执行 PROPFIND 请求
     */
    private suspend fun propfind(url: String, depth: Int): Response = withContext(Dispatchers.IO) {
        val propfindBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <D:propfind xmlns:D="DAV:">
                <D:prop>
                    <D:displayname/>
                    <D:getlastmodified/>
                    <D:getcontentlength/>
                    <D:resourcetype/>
                </D:prop>
            </D:propfind>
        """.trimIndent()
        
        val request = Request.Builder()
            .url(url)
            .method("PROPFIND", propfindBody.toRequestBody("application/xml".toMediaType()))
            .header("Depth", depth.toString())
            .build()
        
        client.newCall(request).execute()
    }
    
    /**
     * 解析 PROPFIND 响应
     */
    private fun parsePropfindResponse(xml: String): List<WebDAVItem> {
        val items = mutableListOf<WebDAVItem>()
        
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(xml.byteInputStream())
            
            val responses = document.getElementsByTagName("D:response")
            
            for (i in 0 until responses.length) {
                val response = responses.item(i) as Element
                
                val href = response.getElementsByTagName("D:href").item(0)?.textContent ?: continue
                
                val displayName = response.getElementsByTagName("D:displayname").item(0)?.textContent
                
                val resourceType = response.getElementsByTagName("D:resourcetype").item(0)
                val isDirectory = resourceType?.getElementsByTagName("D:collection")?.length ?: 0 > 0
                
                val contentLength = response.getElementsByTagName("D:getcontentlength").item(0)?.textContent?.toLongOrNull()
                
                val lastModified = response.getElementsByTagName("D:getlastmodified").item(0)?.textContent
                
                items.add(WebDAVItem(
                    href = href,
                    displayName = displayName ?: href.substringAfterLast('/'),
                    isDirectory = isDirectory,
                    contentLength = contentLength,
                    lastModified = lastModified
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return items
    }
    
    /**
     * 规范化 URL
     */
    private fun normalizeUrl(url: String): String {
        return url.replace("\\", "/")
            .replace("//", "/")
            .let { if (it.startsWith("http")) it else "https://$it" }
    }
}

/**
 * WebDAV 文件/目录项
 */
data class WebDAVItem(
    val href: String,
    val displayName: String,
    val isDirectory: Boolean,
    val contentLength: Long?,
    val lastModified: String?
)

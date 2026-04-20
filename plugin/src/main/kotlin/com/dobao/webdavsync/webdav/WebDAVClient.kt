package com.dobao.webdavsync.webdav

import com.dobao.webdavsync.data.WebDAVConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class WebDAVClient(private var config: WebDAVConfig) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private val PROPFIND_BODY = """<propfind xmlns="DAV:">
  <prop>
    <resourcetype/>
    <getcontentlength/>
    <getlastmodified/>
    <getetag/>
    <displayname/>
  </prop>
</propfind>"""
        private val XML_TYPE = "application/xml; charset=utf-8".toMediaType()
    }

    fun updateConfig(config: WebDAVConfig) { this.config = config }

    private fun buildRequest(url: String, method: String = "GET", body: okhttp3.RequestBody? = null): Request {
        val builder = Request.Builder()
            .url(normalizeUrl(config.url.trimEnd('/') + "/" + url.trimStart('/')))
        if (config.username.isNotBlank()) {
            builder.addHeader("Authorization", Credentials.basic(config.username, config.password))
        }
        return builder.method(method, body).build()
    }

    private fun normalizeUrl(url: String) = url.replace("\\\\", "/")

    suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(buildRequest(path, "HEAD")).execute()
            response.close()
            response.code in 200..299 || response.code == 404
        } catch (e: Exception) { false }
    }

    suspend fun createDirectory(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(buildRequest(path.trimStart('/'), "MKCOL")).execute()
            response.close()
            response.code in 200..299 || response.code == 409
        } catch (e: Exception) { false }
    }

    suspend fun uploadFile(path: String, content: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = content.toRequestBody("application/octet-stream".toMediaType())
            val response = client.newCall(buildRequest(path.trimStart('/'), "PUT", body)).execute()
            response.close()
            if (response.code in 200..299 || response.code == 201) Result.success(Unit)
            else Result.failure(Exception("HTTP ${'$'}{response.code}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun downloadFile(path: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(buildRequest(path.trimStart('/'), "GET")).execute()
            val body = response.body?.bytes() ?: ByteArray(0)
            response.close()
            if (response.code in 200..299) Result.success(body)
            else Result.failure(Exception("HTTP ${'$'}{response.code}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun delete(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(buildRequest(path.trimStart('/'), "DELETE")).execute()
            response.close()
            response.code in 200..299
        } catch (e: Exception) { false }
    }
}

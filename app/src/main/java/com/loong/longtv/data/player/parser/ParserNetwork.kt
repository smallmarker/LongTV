package com.loong.longtv.data.player.parser

import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class FetchResult(
    val finalUrl: String,
    val statusCode: Int,
    val contentType: String,
    val location: String?,
    val body: String? = null
)

@Singleton
class ParserNetwork @Inject constructor() {

    fun fetch(
        url: String,
        headers: Map<String, String>,
        readBody: Boolean = false
    ): FetchResult {
        val connection = openConnection(url, headers)
        return try {
            val statusCode = connection.responseCode
            val contentType = connection.contentType.orEmpty().lowercase()
            val location = connection.getHeaderField("Location")
            val body = if (readBody) readBody(connection) else null
            FetchResult(
                finalUrl = url,
                statusCode = statusCode,
                contentType = contentType,
                location = location,
                body = body
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(
        sourceUrl: String,
        headers: Map<String, String>
    ): HttpURLConnection {
        return (URL(sourceUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = 10_000
            readTimeout = 10_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", headers["User-Agent"] ?: DEFAULT_UA)
            headers.forEach { (key, value) ->
                if (key.equals("User-Agent", ignoreCase = true)) return@forEach
                setRequestProperty(key, value)
            }
        }
    }

    private fun readBody(connection: HttpURLConnection): String? {
        return runCatching {
            buildString {
                BufferedReader(connection.inputStream.bufferedReader()).use { reader ->
                    var total = 0
                    while (true) {
                        val line = reader.readLine() ?: break
                        append(line)
                        append('\n')
                        total += line.length
                        if (total >= MAX_SNIFF_CHARS) break
                    }
                }
            }
        }.getOrNull()
    }

    companion object {
        private const val DEFAULT_UA =
            "Mozilla/5.0 (Linux; Android 13; TV) AppleWebKit/537.36 Chrome/123.0.0.0 Safari/537.36"
        private const val MAX_SNIFF_CHARS = 256 * 1024
    }
}

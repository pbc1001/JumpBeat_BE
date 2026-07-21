package com.jumpbeat.song.util

import com.jumpbeat.common.error.BusinessException
import com.jumpbeat.common.error.ErrorCode
import org.springframework.stereotype.Component
import java.net.URI

@Component
class YouTubeUrlParser {
    fun parseVideoId(url: String): String {
        val uri = runCatching { URI(url.trim()) }
            .getOrElse { throw BusinessException(ErrorCode.INVALID_YOUTUBE_URL) }
        val host = uri.host?.lowercase()?.removePrefix("www.")
            ?: throw BusinessException(ErrorCode.INVALID_YOUTUBE_URL)
        val videoId = when (host) {
            "youtu.be" -> uri.path.trim('/').substringBefore('/')
            "youtube.com", "m.youtube.com", "music.youtube.com" -> parseYouTubePath(uri)
            else -> null
        }

        return videoId
            ?.takeIf { VIDEO_ID.matches(it) }
            ?: throw BusinessException(ErrorCode.INVALID_YOUTUBE_URL)
    }

    private fun parseYouTubePath(uri: URI): String? {
        val segments = uri.path.trim('/').split('/').filter { it.isNotBlank() }
        return when (segments.firstOrNull()) {
            "watch" -> parseQuery(uri.rawQuery)["v"]
            "shorts", "embed", "live" -> segments.getOrNull(1)
            else -> if (uri.path == "/watch") parseQuery(uri.rawQuery)["v"] else null
        }
    }

    private fun parseQuery(query: String?): Map<String, String> =
        query.orEmpty().split('&')
            .mapNotNull { part ->
                val pieces = part.split('=', limit = 2)
                pieces.takeIf { it.size == 2 }?.let { it[0] to it[1] }
            }
            .toMap()

    companion object {
        private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
    }
}

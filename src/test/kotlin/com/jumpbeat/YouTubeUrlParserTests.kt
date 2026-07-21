package com.jumpbeat

import com.jumpbeat.common.error.BusinessException
import com.jumpbeat.song.util.YouTubeUrlParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class YouTubeUrlParserTests {
    private val parser = YouTubeUrlParser()

    @ParameterizedTest
    @ValueSource(
        strings = [
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ?t=12",
            "https://www.youtube.com/shorts/dQw4w9WgXcQ",
            "https://www.youtube.com/embed/dQw4w9WgXcQ",
            "https://music.youtube.com/watch?v=dQw4w9WgXcQ",
        ],
    )
    fun `supported YouTube URL returns video id`(url: String) {
        assertEquals("dQw4w9WgXcQ", parser.parseVideoId(url))
    }

    @ParameterizedTest
    @ValueSource(strings = ["not-a-url", "https://example.com/watch?v=dQw4w9WgXcQ", "https://youtu.be/short"])
    fun `invalid URL is rejected`(url: String) {
        assertThrows(BusinessException::class.java) { parser.parseVideoId(url) }
    }
}

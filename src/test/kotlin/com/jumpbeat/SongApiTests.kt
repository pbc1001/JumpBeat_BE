package com.jumpbeat

import com.jayway.jsonpath.JsonPath
import com.jumpbeat.auth.domain.RefreshTokenRepository
import com.jumpbeat.song.domain.SongRepository
import com.jumpbeat.user.domain.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SongApiTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val songRepository: SongRepository,
    @Autowired private val refreshTokenRepository: RefreshTokenRepository,
    @Autowired private val userRepository: UserRepository,
) {
    @BeforeEach
    fun cleanDatabase() {
        songRepository.deleteAll()
        refreshTokenRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `draft can be synced published and found from public APIs`() {
        val token = signupAndGetAccessToken()
        val draftResult = createDraft(token)
        val songId = JsonPath.read<String>(draftResult, "$.data.id")
        val lyricIds = JsonPath.read<List<String>>(draftResult, "$.data.lyrics[*].id")

        val syncBody = """
            {
              "durationMs": 180000,
              "lyrics": [
                {"id": "${lyricIds[0]}", "startTimeMs": 1000},
                {"id": "${lyricIds[1]}", "startTimeMs": 5000}
              ]
            }
        """.trimIndent()

        mockMvc.patch("/api/v1/songs/drafts/$songId/sync") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = syncBody
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.durationMs") { value(180000) }
            jsonPath("$.data.lyrics[1].startTimeMs") { value(5000) }
        }

        mockMvc.post("/api/v1/songs/drafts/$songId/publish") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.status") { value("PUBLISHED") }
        }

        mockMvc.get("/api/v1/songs/$songId").andExpect {
            status { isOk() }
            jsonPath("$.data.youtubeVideoId") { value("dQw4w9WgXcQ") }
            jsonPath("$.data.lyrics[0].text") { value("첫 번째 가사") }
        }

        mockMvc.get("/api/v1/songs") {
            param("q", "테스트")
            param("language", "KO")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.items.length()") { value(1) }
            jsonPath("$.data.items[0].id") { value(songId) }
        }

        mockMvc.get("/api/v1/songs/duplicates") {
            param("title", "테스트 노래")
            param("artist", "테스트 가수")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.hasDuplicates") { value(true) }
            jsonPath("$.data.items[0].id") { value(songId) }
        }
    }

    @Test
    fun `duplicate draft requires explicit confirmation`() {
        val token = signupAndGetAccessToken()
        val draft = createDraft(token)
        val songId = JsonPath.read<String>(draft, "$.data.id")
        val lyricIds = JsonPath.read<List<String>>(draft, "$.data.lyrics[*].id")
        syncAndPublish(token, songId, lyricIds)

        mockMvc.post("/api/v1/songs/drafts") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = DRAFT_BODY
        }.andExpect {
            status { isConflict() }
            jsonPath("$.error.code") { value("DUPLICATE_CONFIRMATION_REQUIRED") }
            jsonPath("$.error.details.hasDuplicates") { value(true) }
        }
    }

    @Test
    fun `draft creation requires login and valid YouTube URL`() {
        mockMvc.post("/api/v1/songs/drafts") {
            contentType = MediaType.APPLICATION_JSON
            content = DRAFT_BODY
        }.andExpect { status { isUnauthorized() } }

        val token = signupAndGetAccessToken()
        mockMvc.post("/api/v1/songs/drafts") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = DRAFT_BODY.replace("https://youtu.be/dQw4w9WgXcQ", "https://example.com/video")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("INVALID_YOUTUBE_URL") }
        }
    }

    @Test
    fun `owner can edit published song and media change requires resync`() {
        val token = signupAndGetAccessToken()
        val draft = createDraft(token)
        val songId = JsonPath.read<String>(draft, "$.data.id")
        val lyricIds = JsonPath.read<List<String>>(draft, "$.data.lyrics[*].id")
        syncAndPublish(token, songId, lyricIds)

        mockMvc.patch("/api/v1/songs/$songId") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"수정된 제목","difficulty":"HARD"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.title") { value("수정된 제목") }
            jsonPath("$.data.difficulty") { value("HARD") }
            jsonPath("$.data.status") { value("PUBLISHED") }
        }

        mockMvc.patch("/api/v1/songs/$songId") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "youtubeUrl":"https://youtu.be/9bZkp7q19f0",
                  "lyricsText":"새 첫 줄\n새 두 번째 줄\n새 세 번째 줄"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.youtubeVideoId") { value("9bZkp7q19f0") }
            jsonPath("$.data.status") { value("DRAFT") }
            jsonPath("$.data.durationMs") { doesNotExist() }
            jsonPath("$.data.lyrics.length()") { value(3) }
            jsonPath("$.data.lyrics[0].startTimeMs") { doesNotExist() }
        }

        mockMvc.get("/api/v1/songs/$songId")
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `only owner can edit and delete song`() {
        val ownerToken = signupAndGetAccessToken()
        val draft = createDraft(ownerToken)
        val songId = JsonPath.read<String>(draft, "$.data.id")
        val otherToken = signupAndGetAccessToken(
            email = "other@example.com",
            nickname = "다른사용자",
        )

        mockMvc.patch("/api/v1/songs/$songId") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $otherToken")
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"권한 없는 수정"}"""
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.error.code") { value("FORBIDDEN_SONG_ACCESS") }
        }

        mockMvc.delete("/api/v1/songs/$songId") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $otherToken")
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.error.code") { value("FORBIDDEN_SONG_ACCESS") }
        }

        mockMvc.delete("/api/v1/songs/$songId") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $ownerToken")
        }.andExpect { status { isNoContent() } }

        mockMvc.get("/api/v1/songs/drafts/$songId") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $ownerToken")
        }.andExpect { status { isNotFound() } }
    }

    private fun signupAndGetAccessToken(
        email: String = "song@example.com",
        nickname: String = "곡등록자",
    ): String {
        val result = mockMvc.post("/api/v1/auth/signup") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","nickname":"$nickname","password":"password123!"}"""
        }.andExpect { status { isCreated() } }.andReturn()
        return JsonPath.read(result.response.contentAsString, "$.data.accessToken")
    }

    private fun createDraft(token: String): String =
        mockMvc.post("/api/v1/songs/drafts") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = DRAFT_BODY
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.status") { value("DRAFT") }
            jsonPath("$.data.lyrics.length()") { value(2) }
        }.andReturn().response.contentAsString

    private fun syncAndPublish(token: String, songId: String, lyricIds: List<String>) {
        mockMvc.patch("/api/v1/songs/drafts/$songId/sync") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"durationMs":10000,"lyrics":[
                  {"id":"${lyricIds[0]}","startTimeMs":1000},
                  {"id":"${lyricIds[1]}","startTimeMs":5000}
                ]}
            """.trimIndent()
        }.andExpect { status { isOk() } }
        mockMvc.post("/api/v1/songs/drafts/$songId/publish") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }.andExpect { status { isOk() } }
    }

    companion object {
        private val DRAFT_BODY = """
            {
              "title": "테스트 노래",
              "artist": "테스트 가수",
              "youtubeUrl": "https://youtu.be/dQw4w9WgXcQ",
              "language": "KO",
              "difficulty": "NORMAL",
              "lyricsText": "첫 번째 가사\n두 번째 가사",
              "confirmedDuplicate": false
            }
        """.trimIndent()
    }
}

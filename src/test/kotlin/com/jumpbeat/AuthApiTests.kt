package com.jumpbeat

import com.jayway.jsonpath.JsonPath
import com.jumpbeat.auth.domain.RefreshTokenRepository
import com.jumpbeat.user.domain.UserRepository
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val refreshTokenRepository: RefreshTokenRepository,
    @Autowired private val userRepository: UserRepository,
) {
    @BeforeEach
    fun cleanDatabase() {
        refreshTokenRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `signup issues tokens and authenticated user can get profile`() {
        val signupResult = mockMvc.post("/api/v1/auth/signup") {
            contentType = MediaType.APPLICATION_JSON
            content = SIGNUP_BODY
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.user.email") { value("player@example.com") }
            jsonPath("$.data.user.nickname") { value("비트마스터") }
            jsonPath("$.data.accessToken") { isNotEmpty() }
            header { string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")) }
            header { string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")) }
        }.andReturn()

        val accessToken = JsonPath.read<String>(signupResult.response.contentAsString, "$.data.accessToken")

        mockMvc.get("/api/v1/users/me")
            .andExpect { status { isUnauthorized() } }

        mockMvc.get("/api/v1/users/me") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.email") { value("player@example.com") }
            jsonPath("$.data.nickname") { value("비트마스터") }
        }
    }

    @Test
    fun `duplicate email is rejected`() {
        signup()

        mockMvc.post("/api/v1/auth/signup") {
            contentType = MediaType.APPLICATION_JSON
            content = SIGNUP_BODY.replace("비트마스터", "다른닉네임")
        }.andExpect {
            status { isConflict() }
            jsonPath("$.error.code") { value("EMAIL_ALREADY_EXISTS") }
        }
    }

    @Test
    fun `login rejects wrong password`() {
        signup()

        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"player@example.com","password":"wrong-password"}"""
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.error.code") { value("INVALID_CREDENTIALS") }
        }
    }

    @Test
    fun `refresh rotates refresh token and logout expires cookie`() {
        val signupResult = signup()
        val refreshCookie = requireNotNull(signupResult.response.getCookie("refresh_token"))

        val refreshResult = mockMvc.post("/api/v1/auth/refresh") {
            cookie(refreshCookie)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.accessToken") { isNotEmpty() }
            header { string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")) }
        }.andReturn()

        mockMvc.post("/api/v1/auth/refresh") {
            cookie(refreshCookie)
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.error.code") { value("INVALID_REFRESH_TOKEN") }
        }

        val rotatedCookie = requireNotNull(refreshResult.response.getCookie("refresh_token"))
        mockMvc.post("/api/v1/auth/logout") {
            cookie(rotatedCookie)
        }.andExpect {
            status { isNoContent() }
            header { string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")) }
        }
    }

    private fun signup() =
        mockMvc.post("/api/v1/auth/signup") {
            contentType = MediaType.APPLICATION_JSON
            content = SIGNUP_BODY
        }.andExpect {
            status { isCreated() }
        }.andReturn()

    companion object {
        private const val SIGNUP_BODY =
            """{"email":"PLAYER@example.com","nickname":"비트마스터","password":"password123!"}"""
    }
}

package com.jumpbeat.auth.controller

import com.jumpbeat.auth.config.AuthProperties
import com.jumpbeat.auth.dto.AccessTokenResponse
import com.jumpbeat.auth.dto.AuthResponse
import com.jumpbeat.auth.dto.LoginRequest
import com.jumpbeat.auth.dto.SignupRequest
import com.jumpbeat.auth.dto.AuthTokens
import com.jumpbeat.auth.service.AuthService
import com.jumpbeat.common.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@Tag(name = "Auth", description = "회원가입 및 로그인")
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val authProperties: AuthProperties,
) {
    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    fun signup(
        @Valid @RequestBody request: SignupRequest,
        response: HttpServletResponse,
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val tokens = authService.signup(request)
        addRefreshCookie(response, tokens)
        return ResponseEntity.status(201).body(
            ApiResponse.success(
                AuthResponse(
                    user = requireNotNull(tokens.user),
                    accessToken = tokens.accessToken,
                ),
            ),
        )
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        response: HttpServletResponse,
    ): ApiResponse<AuthResponse> {
        val tokens = authService.login(request)
        addRefreshCookie(response, tokens)
        return ApiResponse.success(
            AuthResponse(
                user = requireNotNull(tokens.user),
                accessToken = tokens.accessToken,
            ),
        )
    }

    @Operation(summary = "access token 갱신")
    @PostMapping("/refresh")
    fun refresh(
        @CookieValue(name = REFRESH_COOKIE, required = false) refreshToken: String?,
        response: HttpServletResponse,
    ): ApiResponse<AccessTokenResponse> {
        val tokens = authService.refresh(refreshToken)
        addRefreshCookie(response, tokens)
        return ApiResponse.success(AccessTokenResponse(tokens.accessToken))
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    fun logout(
        @CookieValue(name = REFRESH_COOKIE, required = false) refreshToken: String?,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        authService.logout(refreshToken)
        response.addHeader(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
        return ResponseEntity.noContent().build()
    }

    private fun addRefreshCookie(response: HttpServletResponse, tokens: AuthTokens) {
        val cookie = ResponseCookie.from(REFRESH_COOKIE, tokens.refreshToken)
            .httpOnly(true)
            .secure(authProperties.cookieSecure)
            .sameSite("Lax")
            .path("/api/v1/auth")
            .maxAge(Duration.ofMillis(authProperties.refreshTokenExpirationMs))
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    private fun expiredRefreshCookie(): ResponseCookie =
        ResponseCookie.from(REFRESH_COOKIE, "")
            .httpOnly(true)
            .secure(authProperties.cookieSecure)
            .sameSite("Lax")
            .path("/api/v1/auth")
            .maxAge(Duration.ZERO)
            .build()

    companion object {
        private const val REFRESH_COOKIE = "refresh_token"
    }
}

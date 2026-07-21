package com.jumpbeat.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class SignupRequest(
    @field:NotBlank(message = "이메일을 입력해 주세요.")
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    @field:Size(max = 320, message = "이메일은 320자 이하여야 합니다.")
    val email: String,

    @field:NotBlank(message = "닉네임을 입력해 주세요.")
    @field:Size(min = 2, max = 30, message = "닉네임은 2자 이상 30자 이하여야 합니다.")
    val nickname: String,

    @field:Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하여야 합니다.")
    val password: String,
)

data class LoginRequest(
    @field:NotBlank(message = "이메일을 입력해 주세요.")
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    val email: String,

    @field:NotBlank(message = "비밀번호를 입력해 주세요.")
    val password: String,
)

data class AuthResponse(
    val user: UserResponse,
    val accessToken: String,
)

data class AccessTokenResponse(
    val accessToken: String,
)

data class UserResponse(
    val id: UUID,
    val email: String,
    val nickname: String,
)

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponse? = null,
)

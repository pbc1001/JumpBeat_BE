package com.jumpbeat.auth.service

import com.jumpbeat.auth.config.AuthProperties
import com.jumpbeat.auth.domain.RefreshToken
import com.jumpbeat.auth.domain.RefreshTokenRepository
import com.jumpbeat.auth.dto.AuthTokens
import com.jumpbeat.auth.dto.LoginRequest
import com.jumpbeat.auth.dto.SignupRequest
import com.jumpbeat.auth.dto.UserResponse
import com.jumpbeat.auth.jwt.JwtTokenProvider
import com.jumpbeat.common.error.BusinessException
import com.jumpbeat.common.error.ErrorCode
import com.jumpbeat.user.domain.User
import com.jumpbeat.user.domain.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.Locale
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val authProperties: AuthProperties,
) {
    private val secureRandom = SecureRandom()

    @Transactional
    fun signup(request: SignupRequest): AuthTokens {
        val email = normalizeEmail(request.email)
        val nickname = request.nickname.trim()

        if (userRepository.existsByEmail(email)) {
            throw BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS)
        }
        if (userRepository.existsByNickname(nickname)) {
            throw BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS)
        }

        val user = userRepository.save(
            User(
                email = email,
                nickname = nickname,
                passwordHash = requireNotNull(passwordEncoder.encode(request.password)),
            ),
        )
        return issueTokens(user, includeUser = true)
    }

    @Transactional
    fun login(request: LoginRequest): AuthTokens {
        val user = userRepository.findByEmail(normalizeEmail(request.email))
            ?: throw BusinessException(ErrorCode.INVALID_CREDENTIALS)
        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw BusinessException(ErrorCode.INVALID_CREDENTIALS)
        }
        return issueTokens(user, includeUser = true)
    }

    @Transactional
    fun refresh(rawRefreshToken: String?): AuthTokens {
        val rawToken = rawRefreshToken?.takeIf { it.isNotBlank() }
            ?: throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)
        val storedToken = refreshTokenRepository.findByTokenHash(hashToken(rawToken))
            ?: throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)
        val now = Instant.now()
        if (!storedToken.isUsable(now)) {
            throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)
        }

        storedToken.revoke(now)
        return issueTokens(storedToken.user, includeUser = false, now = now)
    }

    @Transactional
    fun logout(rawRefreshToken: String?) {
        if (rawRefreshToken.isNullOrBlank()) return
        refreshTokenRepository.findByTokenHash(hashToken(rawRefreshToken))
            ?.takeIf { it.revokedAt == null }
            ?.revoke(Instant.now())
    }

    @Transactional(readOnly = true)
    fun getUser(userId: UUID): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        return user.toResponse()
    }

    private fun issueTokens(
        user: User,
        includeUser: Boolean,
        now: Instant = Instant.now(),
    ): AuthTokens {
        val rawRefreshToken = generateRefreshToken()
        refreshTokenRepository.save(
            RefreshToken(
                user = user,
                tokenHash = hashToken(rawRefreshToken),
                expiresAt = now.plusMillis(authProperties.refreshTokenExpirationMs),
                createdAt = now,
            ),
        )

        return AuthTokens(
            accessToken = jwtTokenProvider.createAccessToken(user, now),
            refreshToken = rawRefreshToken,
            user = if (includeUser) user.toResponse() else null,
        )
    }

    private fun generateRefreshToken(): String {
        val bytes = ByteArray(64)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hashToken(token: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun normalizeEmail(email: String): String = email.trim().lowercase(Locale.ROOT)

    private fun User.toResponse(): UserResponse =
        UserResponse(
            id = requireNotNull(id),
            email = email,
            nickname = nickname,
        )
}

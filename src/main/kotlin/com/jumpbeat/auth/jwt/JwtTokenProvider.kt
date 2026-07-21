package com.jumpbeat.auth.jwt

import com.jumpbeat.auth.config.AuthProperties
import com.jumpbeat.user.domain.User
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    private val properties: AuthProperties,
) {
    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(properties.accessTokenSecret.toByteArray(StandardCharsets.UTF_8))
    }

    fun createAccessToken(user: User, now: Instant = Instant.now()): String =
        Jwts.builder()
            .subject(requireNotNull(user.id).toString())
            .claim("role", user.role.name)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(properties.accessTokenExpirationMs)))
            .signWith(signingKey)
            .compact()

    fun parse(token: String): JwtPayload {
        val claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload

        val userId = runCatching { UUID.fromString(claims.subject) }
            .getOrElse { throw JwtException("Invalid subject", it) }
        val role = claims["role"] as? String ?: throw JwtException("Missing role")
        return JwtPayload(userId, role)
    }
}

data class JwtPayload(
    val userId: UUID,
    val role: String,
)

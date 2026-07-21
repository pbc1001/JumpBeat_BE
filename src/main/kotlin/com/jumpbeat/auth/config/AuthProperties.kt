package com.jumpbeat.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("jumpbeat.auth")
data class AuthProperties(
    val accessTokenSecret: String,
    val accessTokenExpirationMs: Long,
    val refreshTokenExpirationMs: Long,
    val cookieSecure: Boolean,
)

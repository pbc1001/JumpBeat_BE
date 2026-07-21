package com.jumpbeat.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig(
    @Value("\${jumpbeat.frontend-origin}") private val frontendOrigin: String,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .cors { }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/docs", "/docs/**", "/api-docs", "/api-docs/**").permitAll()
                it.requestMatchers("/api/v1/health", "/error").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/v1/songs/**").permitAll()
                it.requestMatchers("/api/v1/auth/**").permitAll()
                it.anyRequest().authenticated()
            }
            .build()

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf(frontendOrigin)
            allowedMethods = listOf("GET", "POST", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type")
            allowCredentials = true
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}

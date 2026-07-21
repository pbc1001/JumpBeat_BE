package com.jumpbeat.user.controller

import com.jumpbeat.auth.dto.UserResponse
import com.jumpbeat.auth.service.AuthService
import com.jumpbeat.common.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "User", description = "사용자 정보")
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val authService: AuthService,
) {
    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    fun me(authentication: Authentication): ApiResponse<UserResponse> =
        ApiResponse.success(authService.getUser(UUID.fromString(authentication.name)))
}

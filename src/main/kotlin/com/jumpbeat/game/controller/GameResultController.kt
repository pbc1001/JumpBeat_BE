package com.jumpbeat.game.controller

import com.jumpbeat.common.api.ApiResponse
import com.jumpbeat.game.dto.CreateGameResultRequest
import com.jumpbeat.game.dto.GameResultResponse
import com.jumpbeat.game.dto.SongRankingResponse
import com.jumpbeat.game.service.GameResultService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class GameResultController(
    private val gameResultService: GameResultService,
) {
    @PostMapping("/api/v1/game-results")
    fun create(
        authentication: Authentication,
        @Valid @RequestBody request: CreateGameResultRequest,
    ): ResponseEntity<ApiResponse<GameResultResponse>> =
        ResponseEntity.status(201).body(ApiResponse.success(gameResultService.create(authentication.userId(), request)))

    @GetMapping("/api/v1/songs/{songId}/ranking")
    fun ranking(@PathVariable songId: UUID): ApiResponse<SongRankingResponse> =
        ApiResponse.success(gameResultService.ranking(songId))

    private fun Authentication.userId(): UUID = UUID.fromString(name)
}

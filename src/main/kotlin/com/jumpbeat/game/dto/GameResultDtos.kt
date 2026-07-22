package com.jumpbeat.game.dto

import com.jumpbeat.game.domain.GameMode
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.util.UUID

data class CreateGameResultRequest(
    @field:NotNull val songId: UUID,
    @field:NotNull val mode: GameMode,
    @field:Min(0) val correctCount: Int,
    @field:Min(0) val wrongCount: Int,
    @field:Min(0) val missCount: Int,
    @field:Min(1) val totalCount: Int,
    @field:Min(0) val playTimeMs: Int,
)

data class GameResultResponse(
    val id: UUID,
    val score: Int,
    val accuracy: BigDecimal,
)

data class RankingEntryResponse(
    val rank: Int,
    val nickname: String,
)

data class SongRankingResponse(
    val songId: UUID,
    val rankings: List<RankingEntryResponse>,
)

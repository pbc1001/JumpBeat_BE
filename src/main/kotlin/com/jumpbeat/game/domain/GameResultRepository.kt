package com.jumpbeat.game.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GameResultRepository : JpaRepository<GameResult, UUID> {
    fun findAllBySong_IdOrderByScoreDescAccuracyDescPlayTimeMsAscPlayedAtAsc(songId: UUID): List<GameResult>
}

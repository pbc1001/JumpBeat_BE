package com.jumpbeat.game.service

import com.jumpbeat.common.error.BusinessException
import com.jumpbeat.common.error.ErrorCode
import com.jumpbeat.game.domain.GameResult
import com.jumpbeat.game.domain.GameResultRepository
import com.jumpbeat.game.dto.CreateGameResultRequest
import com.jumpbeat.game.dto.GameResultResponse
import com.jumpbeat.game.dto.RankingEntryResponse
import com.jumpbeat.game.dto.SongRankingResponse
import com.jumpbeat.song.domain.SongRepository
import com.jumpbeat.song.domain.SongStatus
import com.jumpbeat.user.domain.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Service
class GameResultService(
    private val gameResultRepository: GameResultRepository,
    private val songRepository: SongRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun create(userId: UUID, request: CreateGameResultRequest): GameResultResponse {
        val user = userRepository.findById(userId).orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        val song = songRepository.findByIdAndStatusAndDeletedAtIsNull(request.songId, SongStatus.PUBLISHED)
            ?: throw BusinessException(ErrorCode.SONG_NOT_FOUND)
        val processed = request.correctCount + request.wrongCount + request.missCount
        if (processed > request.totalCount || request.totalCount > song.lyrics.size) {
            throw BusinessException(ErrorCode.VALIDATION_FAILED)
        }

        val accuracy = percentage(request.correctCount, processed)
        val completionRate = percentage(processed, request.totalCount)
        val saved = gameResultRepository.save(
            GameResult(
                user = user,
                song = song,
                mode = request.mode,
                score = request.correctCount * 100,
                accuracy = accuracy,
                correctCount = request.correctCount,
                missCount = request.wrongCount + request.missCount,
                maxCombo = request.correctCount,
                completionRate = completionRate,
                playTimeMs = request.playTimeMs,
            ),
        )
        return GameResultResponse(requireNotNull(saved.id), saved.score, saved.accuracy)
    }

    @Transactional(readOnly = true)
    fun ranking(songId: UUID): SongRankingResponse {
        songRepository.findByIdAndStatusAndDeletedAtIsNull(songId, SongStatus.PUBLISHED)
            ?: throw BusinessException(ErrorCode.SONG_NOT_FOUND)
        val rankings = gameResultRepository
            .findAllBySong_IdOrderByScoreDescAccuracyDescPlayTimeMsAscPlayedAtAsc(songId)
            .distinctBy { requireNotNull(it.user.id) }
            .take(3)
            .mapIndexed { index, result -> RankingEntryResponse(index + 1, result.user.nickname) }
        return SongRankingResponse(songId, rankings)
    }

    private fun percentage(numerator: Int, denominator: Int): BigDecimal =
        if (denominator == 0) BigDecimal.ZERO.setScale(2)
        else BigDecimal(numerator * 100).divide(BigDecimal(denominator), 2, RoundingMode.HALF_UP)
}

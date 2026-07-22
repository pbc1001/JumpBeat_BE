package com.jumpbeat.game.domain

import com.jumpbeat.song.domain.Song
import com.jumpbeat.user.domain.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class GameMode { CONTINUE, SURVIVAL }

@Entity
@Table(name = "game_results")
class GameResult(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "song_id", nullable = false)
    val song: Song,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val mode: GameMode,

    @Column(nullable = false)
    val score: Int,

    @Column(nullable = false, precision = 5, scale = 2)
    val accuracy: BigDecimal,

    @Column(name = "correct_count", nullable = false)
    val correctCount: Int,

    @Column(name = "miss_count", nullable = false)
    val missCount: Int,

    @Column(name = "max_combo", nullable = false)
    val maxCombo: Int,

    @Column(name = "completion_rate", nullable = false, precision = 5, scale = 2)
    val completionRate: BigDecimal,

    @Column(name = "play_time_ms", nullable = false)
    val playTimeMs: Int,

    @Column(name = "played_at", nullable = false, updatable = false)
    val playedAt: Instant = Instant.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
)

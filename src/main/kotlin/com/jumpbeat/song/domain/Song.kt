package com.jumpbeat.song.domain

import com.jumpbeat.user.domain.User
import jakarta.persistence.CascadeType
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
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "songs")
class Song(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    var creator: User,

    @Column(nullable = false, length = 150)
    var title: String,

    @Column(name = "normalized_title", nullable = false, length = 150)
    var normalizedTitle: String,

    @Column(nullable = false, length = 100)
    var artist: String,

    @Column(name = "normalized_artist", nullable = false, length = 100)
    var normalizedArtist: String,

    @Column(name = "youtube_video_id", nullable = false, length = 20)
    var youtubeVideoId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var language: SongLanguage,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var difficulty: SongDifficulty,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: SongStatus = SongStatus.DRAFT,

    @Column(name = "duration_ms")
    var durationMs: Int? = null,

    @Column(name = "play_count", nullable = false)
    var playCount: Long = 0,

    @Column(name = "published_at")
    var publishedAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
) {
    @OneToMany(mappedBy = "song", cascade = [CascadeType.ALL], orphanRemoval = true)
    val lyrics: MutableList<LyricLine> = mutableListOf()

    fun addLyric(text: String, lineOrder: Int) {
        lyrics += LyricLine(song = this, text = text, lineOrder = lineOrder)
    }

    fun publish(now: Instant) {
        status = SongStatus.PUBLISHED
        publishedAt = now
        updatedAt = now
    }
}

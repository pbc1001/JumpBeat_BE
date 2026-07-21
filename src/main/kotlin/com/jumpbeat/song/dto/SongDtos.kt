package com.jumpbeat.song.dto

import com.jumpbeat.song.domain.SongDifficulty
import com.jumpbeat.song.domain.SongLanguage
import com.jumpbeat.song.domain.SongSort
import com.jumpbeat.song.domain.SongStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateSongDraftRequest(
    @field:NotBlank(message = "노래 제목을 입력해 주세요.")
    @field:Size(max = 150, message = "노래 제목은 150자 이하여야 합니다.")
    val title: String,

    @field:NotBlank(message = "가수명을 입력해 주세요.")
    @field:Size(max = 100, message = "가수명은 100자 이하여야 합니다.")
    val artist: String,

    @field:NotBlank(message = "YouTube URL을 입력해 주세요.")
    @field:Size(max = 500, message = "YouTube URL이 너무 깁니다.")
    val youtubeUrl: String,

    val language: SongLanguage,
    val difficulty: SongDifficulty,

    @field:NotBlank(message = "가사를 입력해 주세요.")
    @field:Size(max = 100000, message = "가사가 너무 깁니다.")
    val lyricsText: String,

    val confirmedDuplicate: Boolean = false,
)

data class SaveSongSyncRequest(
    @field:Min(value = 1, message = "영상 길이는 1ms 이상이어야 합니다.")
    val durationMs: Int,

    @field:NotEmpty(message = "가사 싱크를 입력해 주세요.")
    @field:Valid
    val lyrics: List<LyricSyncRequest>,
)

data class LyricSyncRequest(
    val id: UUID,

    @field:Min(value = 0, message = "가사 시작 시간은 0 이상이어야 합니다.")
    val startTimeMs: Int,
)

data class SongListResponse(
    val items: List<SongSummaryResponse>,
    val nextCursor: String?,
)

data class SongSummaryResponse(
    val id: UUID,
    val title: String,
    val artist: String,
    val youtubeVideoId: String,
    val language: SongLanguage,
    val difficulty: SongDifficulty,
    val lyricLineCount: Int,
    val playCount: Long,
    val creator: SongCreatorResponse,
    val publishedAt: Instant,
)

data class SongCreatorResponse(
    val id: UUID,
    val nickname: String,
)

data class SongDetailResponse(
    val id: UUID,
    val title: String,
    val artist: String,
    val youtubeVideoId: String,
    val language: SongLanguage,
    val difficulty: SongDifficulty,
    val status: SongStatus,
    val durationMs: Int?,
    val lyrics: List<LyricLineResponse>,
)

data class LyricLineResponse(
    val id: UUID,
    val lineOrder: Int,
    val text: String,
    val startTimeMs: Int?,
)

data class DuplicateSongsResponse(
    val hasDuplicates: Boolean,
    val items: List<DuplicateSongResponse>,
)

data class DuplicateSongResponse(
    val id: UUID,
    val title: String,
    val artist: String,
    val youtubeVideoId: String,
)

data class SongQuery(
    val q: String?,
    val language: SongLanguage?,
    val difficulty: SongDifficulty?,
    val sort: SongSort,
    val cursor: String?,
    @field:Min(1)
    @field:Max(50)
    val limit: Int,
)

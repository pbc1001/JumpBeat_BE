package com.jumpbeat.song.controller

import com.jumpbeat.common.api.ApiResponse
import com.jumpbeat.common.error.BusinessException
import com.jumpbeat.common.error.ErrorCode
import com.jumpbeat.song.domain.SongDifficulty
import com.jumpbeat.song.domain.SongLanguage
import com.jumpbeat.song.domain.SongSort
import com.jumpbeat.song.dto.CreateSongDraftRequest
import com.jumpbeat.song.dto.DuplicateSongsResponse
import com.jumpbeat.song.dto.SaveSongSyncRequest
import com.jumpbeat.song.dto.SongDetailResponse
import com.jumpbeat.song.dto.SongListResponse
import com.jumpbeat.song.dto.SongQuery
import com.jumpbeat.song.service.SongService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "Song", description = "공개 곡 조회 및 곡 제작")
@Validated
@RestController
@RequestMapping("/api/v1/songs")
class SongController(
    private val songService: SongService,
) {
    @Operation(summary = "공개 곡 목록 조회")
    @GetMapping
    fun getSongs(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) language: SongLanguage?,
        @RequestParam(required = false) difficulty: SongDifficulty?,
        @RequestParam(defaultValue = "LATEST") sort: SongSort,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ApiResponse<SongListResponse> =
        ApiResponse.success(
            songService.getSongs(
                SongQuery(
                    q,
                    language,
                    difficulty,
                    sort,
                    cursor,
                    limit.takeIf { it in 1..50 } ?: throw BusinessException(ErrorCode.VALIDATION_FAILED),
                ),
            ),
        )

    @Operation(summary = "중복 곡 후보 조회")
    @GetMapping("/duplicates")
    fun duplicates(
        @RequestParam title: String,
        @RequestParam(required = false) artist: String?,
    ): ApiResponse<DuplicateSongsResponse> =
        ApiResponse.success(songService.findDuplicates(title, artist))

    @Operation(summary = "공개 곡 상세 조회")
    @GetMapping("/{songId}")
    fun getSong(@PathVariable songId: UUID): ApiResponse<SongDetailResponse> =
        ApiResponse.success(songService.getPublishedSong(songId))

    @Operation(summary = "곡 초안 생성")
    @PostMapping("/drafts")
    fun createDraft(
        authentication: Authentication,
        @Valid @RequestBody request: CreateSongDraftRequest,
    ): ResponseEntity<ApiResponse<SongDetailResponse>> =
        ResponseEntity.status(201).body(
            ApiResponse.success(songService.createDraft(authentication.userId(), request)),
        )

    @Operation(summary = "내 곡 초안 조회")
    @GetMapping("/drafts/{songId}")
    fun getDraft(
        authentication: Authentication,
        @PathVariable songId: UUID,
    ): ApiResponse<SongDetailResponse> =
        ApiResponse.success(songService.getDraft(authentication.userId(), songId))

    @Operation(summary = "가사 싱크 저장")
    @PatchMapping("/drafts/{songId}/sync")
    fun saveSync(
        authentication: Authentication,
        @PathVariable songId: UUID,
        @Valid @RequestBody request: SaveSongSyncRequest,
    ): ApiResponse<SongDetailResponse> =
        ApiResponse.success(songService.saveSync(authentication.userId(), songId, request))

    @Operation(summary = "곡 최종 공개")
    @PostMapping("/drafts/{songId}/publish")
    fun publish(
        authentication: Authentication,
        @PathVariable songId: UUID,
    ): ApiResponse<SongDetailResponse> =
        ApiResponse.success(songService.publish(authentication.userId(), songId))

    private fun Authentication.userId(): UUID = UUID.fromString(name)
}

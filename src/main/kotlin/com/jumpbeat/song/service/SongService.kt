package com.jumpbeat.song.service

import com.jumpbeat.common.error.BusinessException
import com.jumpbeat.common.error.ErrorCode
import com.jumpbeat.song.domain.Song
import com.jumpbeat.song.domain.SongRepository
import com.jumpbeat.song.domain.SongSort
import com.jumpbeat.song.domain.SongStatus
import com.jumpbeat.song.dto.CreateSongDraftRequest
import com.jumpbeat.song.dto.DuplicateSongResponse
import com.jumpbeat.song.dto.DuplicateSongsResponse
import com.jumpbeat.song.dto.LyricLineResponse
import com.jumpbeat.song.dto.SaveSongSyncRequest
import com.jumpbeat.song.dto.SongCreatorResponse
import com.jumpbeat.song.dto.SongDetailResponse
import com.jumpbeat.song.dto.SongListResponse
import com.jumpbeat.song.dto.SongQuery
import com.jumpbeat.song.dto.SongSummaryResponse
import com.jumpbeat.song.dto.UpdateSongRequest
import com.jumpbeat.song.util.YouTubeUrlParser
import com.jumpbeat.user.domain.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import java.util.Locale
import java.util.UUID

@Service
class SongService(
    private val songRepository: SongRepository,
    private val userRepository: UserRepository,
    private val youTubeUrlParser: YouTubeUrlParser,
) {
    @Transactional(readOnly = true)
    fun getSongs(query: SongQuery): SongListResponse {
        val pageNumber = decodeCursor(query.cursor)
        val sort = when (query.sort) {
            SongSort.LATEST -> Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"))
            SongSort.POPULAR -> Sort.by(
                Sort.Order.desc("playCount"),
                Sort.Order.desc("publishedAt"),
                Sort.Order.desc("id"),
            )
        }
        val specification = publicSongSpecification(query)
        val page = songRepository.findAll(specification, PageRequest.of(pageNumber, query.limit, sort))

        return SongListResponse(
            items = page.content.map { it.toSummaryResponse() },
            nextCursor = if (page.hasNext()) encodeCursor(pageNumber + 1) else null,
        )
    }

    @Transactional(readOnly = true)
    fun getPublishedSong(songId: UUID): SongDetailResponse {
        val song = songRepository.findByIdAndStatusAndDeletedAtIsNull(songId, SongStatus.PUBLISHED)
            ?: throw BusinessException(ErrorCode.SONG_NOT_FOUND)
        return song.toDetailResponse()
    }

    @Transactional(readOnly = true)
    fun findDuplicates(title: String, artist: String?): DuplicateSongsResponse {
        if (title.isBlank()) throw BusinessException(ErrorCode.VALIDATION_FAILED)
        val songs = duplicateSongs(title)
        val normalizedArtist = artist?.let(::normalizeText)
        val sorted = songs.sortedByDescending { it.normalizedArtist == normalizedArtist }
        return DuplicateSongsResponse(
            hasDuplicates = sorted.isNotEmpty(),
            items = sorted.map { it.toDuplicateResponse() },
        )
    }

    @Transactional
    fun createDraft(userId: UUID, request: CreateSongDraftRequest): SongDetailResponse {
        val duplicates = duplicateSongs(request.title)
        if (duplicates.isNotEmpty() && !request.confirmedDuplicate) {
            throw BusinessException(
                ErrorCode.DUPLICATE_CONFIRMATION_REQUIRED,
                DuplicateSongsResponse(true, duplicates.map { it.toDuplicateResponse() }),
            )
        }

        val lyricTexts = request.lyricsText.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()
        if (lyricTexts.isEmpty() || lyricTexts.size > MAX_LYRIC_LINES || lyricTexts.any { it.length > 500 }) {
            throw BusinessException(ErrorCode.VALIDATION_FAILED)
        }

        val creator = userRepository.findById(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        val title = request.title.trim()
        val artist = request.artist.trim()
        val song = Song(
            creator = creator,
            title = title,
            normalizedTitle = normalizeText(title),
            artist = artist,
            normalizedArtist = normalizeText(artist),
            youtubeVideoId = youTubeUrlParser.parseVideoId(request.youtubeUrl),
            language = request.language,
            difficulty = request.difficulty,
        )
        lyricTexts.forEachIndexed { index, text -> song.addLyric(text, index) }
        return songRepository.save(song).toDetailResponse()
    }

    @Transactional(readOnly = true)
    fun getDraft(userId: UUID, songId: UUID): SongDetailResponse =
        ownedDraft(userId, songId).toDetailResponse()

    @Transactional
    fun saveSync(userId: UUID, songId: UUID, request: SaveSongSyncRequest): SongDetailResponse {
        val song = ownedDraft(userId, songId)
        val lyricById = song.lyrics.associateBy { requireNotNull(it.id) }
        if (request.lyrics.size != lyricById.size || request.lyrics.map { it.id }.toSet() != lyricById.keys) {
            throw BusinessException(ErrorCode.INVALID_LYRIC_TIMELINE)
        }

        val orderedSync = request.lyrics.sortedBy { lyricById.getValue(it.id).lineOrder }
        val times = orderedSync.map { it.startTimeMs }
        if (times.zipWithNext().any { (current, next) -> current >= next } || times.any { it >= request.durationMs }) {
            throw BusinessException(ErrorCode.INVALID_LYRIC_TIMELINE)
        }

        val now = Instant.now()
        orderedSync.forEach {
            lyricById.getValue(it.id).apply {
                startTimeMs = it.startTimeMs
                updatedAt = now
            }
        }
        song.durationMs = request.durationMs
        song.updatedAt = now
        return song.toDetailResponse()
    }

    @Transactional
    fun publish(userId: UUID, songId: UUID): SongDetailResponse {
        val song = ownedDraft(userId, songId)
        val durationMs = song.durationMs ?: throw BusinessException(ErrorCode.INVALID_LYRIC_TIMELINE)
        val times = song.lyrics.sortedBy { it.lineOrder }.map { it.startTimeMs }
        if (times.any { it == null } || times.filterNotNull().zipWithNext().any { (a, b) -> a >= b } ||
            times.filterNotNull().any { it >= durationMs }
        ) {
            throw BusinessException(ErrorCode.INVALID_LYRIC_TIMELINE)
        }
        song.publish(Instant.now())
        return song.toDetailResponse()
    }

    @Transactional
    fun updateSong(userId: UUID, songId: UUID, request: UpdateSongRequest): SongDetailResponse {
        val song = ownedSong(userId, songId)
        if (request.title == null && request.artist == null && request.youtubeUrl == null &&
            request.language == null && request.difficulty == null && request.lyricsText == null
        ) {
            throw BusinessException(ErrorCode.VALIDATION_FAILED)
        }

        request.title?.trim()?.let { newTitle ->
            if (newTitle.isEmpty()) throw BusinessException(ErrorCode.VALIDATION_FAILED)
            val duplicates = duplicateSongs(newTitle).filter { it.id != song.id }
            if (duplicates.isNotEmpty() && !request.confirmedDuplicate) {
                throw BusinessException(
                    ErrorCode.DUPLICATE_CONFIRMATION_REQUIRED,
                    DuplicateSongsResponse(true, duplicates.map { it.toDuplicateResponse() }),
                )
            }
            song.title = newTitle
            song.normalizedTitle = normalizeText(newTitle)
        }
        request.artist?.trim()?.let { newArtist ->
            if (newArtist.isEmpty()) throw BusinessException(ErrorCode.VALIDATION_FAILED)
            song.artist = newArtist
            song.normalizedArtist = normalizeText(newArtist)
        }
        request.language?.let { song.language = it }
        request.difficulty?.let { song.difficulty = it }

        request.youtubeUrl?.let { newUrl ->
            val newVideoId = youTubeUrlParser.parseVideoId(newUrl)
            if (newVideoId != song.youtubeVideoId) {
                song.youtubeVideoId = newVideoId
                song.resetSync()
            }
        }
        request.lyricsText?.let { lyricsText ->
            song.replaceLyrics(parseLyricLines(lyricsText))
        }
        song.updatedAt = Instant.now()
        return songRepository.saveAndFlush(song).toDetailResponse()
    }

    @Transactional
    fun deleteSong(userId: UUID, songId: UUID) {
        ownedSong(userId, songId).softDelete(Instant.now())
    }

    private fun ownedDraft(userId: UUID, songId: UUID): Song {
        val song = ownedSong(userId, songId)
        if (song.status != SongStatus.DRAFT) throw BusinessException(ErrorCode.INVALID_SONG_STATUS)
        return song
    }

    private fun ownedSong(userId: UUID, songId: UUID): Song {
        val song = songRepository.findById(songId).orElseThrow { BusinessException(ErrorCode.SONG_NOT_FOUND) }
        if (song.deletedAt != null) throw BusinessException(ErrorCode.SONG_NOT_FOUND)
        if (song.creator.id != userId) throw BusinessException(ErrorCode.FORBIDDEN_SONG_ACCESS)
        return song
    }

    private fun duplicateSongs(title: String): List<Song> =
        songRepository.findTop10ByStatusAndNormalizedTitleOrderByPublishedAtDesc(
            SongStatus.PUBLISHED,
            normalizeText(title),
        )

    private fun parseLyricLines(lyricsText: String): List<String> {
        val lines = lyricsText.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()
        if (lines.isEmpty() || lines.size > MAX_LYRIC_LINES || lines.any { it.length > 500 }) {
            throw BusinessException(ErrorCode.VALIDATION_FAILED)
        }
        return lines
    }

    private fun publicSongSpecification(query: SongQuery): Specification<Song> =
        Specification { root, _, criteriaBuilder ->
            val predicates = mutableListOf(
                criteriaBuilder.equal(root.get<SongStatus>("status"), SongStatus.PUBLISHED),
                criteriaBuilder.isNull(root.get<Instant>("deletedAt")),
            )
            query.q?.trim()?.takeIf { it.isNotEmpty() }?.let { keyword ->
                val pattern = "%${keyword.lowercase(Locale.ROOT)}%"
                predicates += criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("artist")), pattern),
                )
            }
            query.language?.let { predicates += criteriaBuilder.equal(root.get<Any>("language"), it) }
            query.difficulty?.let { predicates += criteriaBuilder.equal(root.get<Any>("difficulty"), it) }
            criteriaBuilder.and(*predicates.toTypedArray())
        }

    private fun normalizeText(value: String): String =
        value.trim().lowercase(Locale.ROOT).replace(NON_ALPHANUMERIC, "")

    private fun decodeCursor(cursor: String?): Int {
        if (cursor.isNullOrBlank()) return 0
        return runCatching {
            String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).toInt()
        }.getOrElse { throw BusinessException(ErrorCode.VALIDATION_FAILED) }
            .takeIf { it >= 0 }
            ?: throw BusinessException(ErrorCode.VALIDATION_FAILED)
    }

    private fun encodeCursor(page: Int): String =
        Base64.getUrlEncoder().withoutPadding()
            .encodeToString(page.toString().toByteArray(StandardCharsets.UTF_8))

    private fun Song.toSummaryResponse(): SongSummaryResponse =
        SongSummaryResponse(
            id = requireNotNull(id),
            title = title,
            artist = artist,
            youtubeVideoId = youtubeVideoId,
            language = language,
            difficulty = difficulty,
            lyricLineCount = lyrics.size,
            playCount = playCount,
            creator = SongCreatorResponse(requireNotNull(creator.id), creator.nickname),
            publishedAt = requireNotNull(publishedAt),
        )

    private fun Song.toDetailResponse(): SongDetailResponse =
        SongDetailResponse(
            id = requireNotNull(id),
            title = title,
            artist = artist,
            youtubeVideoId = youtubeVideoId,
            language = language,
            difficulty = difficulty,
            status = status,
            durationMs = durationMs,
            lyrics = lyrics.sortedBy { it.lineOrder }.map {
                LyricLineResponse(
                    id = requireNotNull(it.id),
                    lineOrder = it.lineOrder,
                    text = it.text,
                    startTimeMs = it.startTimeMs,
                )
            },
        )

    private fun Song.toDuplicateResponse(): DuplicateSongResponse =
        DuplicateSongResponse(requireNotNull(id), title, artist, youtubeVideoId)

    companion object {
        private const val MAX_LYRIC_LINES = 500
        private val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]")
    }
}

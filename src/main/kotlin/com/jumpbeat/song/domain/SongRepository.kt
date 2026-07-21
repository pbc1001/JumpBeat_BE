package com.jumpbeat.song.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.util.UUID

interface SongRepository : JpaRepository<Song, UUID>, JpaSpecificationExecutor<Song> {
    fun findTop10ByStatusAndNormalizedTitleOrderByPublishedAtDesc(
        status: SongStatus,
        normalizedTitle: String,
    ): List<Song>

    fun findByIdAndStatusAndDeletedAtIsNull(id: UUID, status: SongStatus): Song?

    fun findByIdAndCreatorIdAndDeletedAtIsNull(id: UUID, creatorId: UUID): Song?
}

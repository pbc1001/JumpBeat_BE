package com.jumpbeat.song.domain

enum class SongLanguage {
    KO,
    EN,
    JA,
    OTHER,
}

enum class SongDifficulty {
    EASY,
    NORMAL,
    HARD,
}

enum class SongStatus {
    DRAFT,
    PUBLISHED,
    HIDDEN,
    UNAVAILABLE,
}

enum class SongSort {
    LATEST,
    POPULAR,
}

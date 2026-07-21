CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(320) NOT NULL UNIQUE,
    nickname VARCHAR(30) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE TABLE songs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(150) NOT NULL,
    normalized_title VARCHAR(150) NOT NULL,
    artist VARCHAR(100) NOT NULL,
    normalized_artist VARCHAR(100) NOT NULL,
    youtube_video_id VARCHAR(20) NOT NULL,
    language VARCHAR(20) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    duration_ms INTEGER,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_songs_language CHECK (language IN ('KO', 'EN', 'JA', 'OTHER')),
    CONSTRAINT chk_songs_difficulty CHECK (difficulty IN ('EASY', 'NORMAL', 'HARD')),
    CONSTRAINT chk_songs_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'HIDDEN', 'UNAVAILABLE')),
    CONSTRAINT chk_songs_duration CHECK (duration_ms IS NULL OR duration_ms > 0)
);

CREATE TABLE lyric_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    song_id UUID NOT NULL REFERENCES songs(id) ON DELETE CASCADE,
    line_order INTEGER NOT NULL,
    text VARCHAR(500) NOT NULL,
    start_time_ms INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_lyric_lines_song_order UNIQUE (song_id, line_order),
    CONSTRAINT chk_lyric_lines_order CHECK (line_order >= 0),
    CONSTRAINT chk_lyric_lines_start_time CHECK (start_time_ms IS NULL OR start_time_ms >= 0)
);

CREATE TABLE game_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    song_id UUID NOT NULL REFERENCES songs(id),
    mode VARCHAR(20) NOT NULL,
    score INTEGER NOT NULL,
    accuracy NUMERIC(5, 2) NOT NULL,
    correct_count INTEGER NOT NULL,
    miss_count INTEGER NOT NULL,
    max_combo INTEGER NOT NULL,
    completion_rate NUMERIC(5, 2) NOT NULL,
    play_time_ms INTEGER NOT NULL,
    played_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_game_results_mode CHECK (mode IN ('CONTINUE', 'SURVIVAL')),
    CONSTRAINT chk_game_results_score CHECK (score >= 0),
    CONSTRAINT chk_game_results_accuracy CHECK (accuracy BETWEEN 0 AND 100),
    CONSTRAINT chk_game_results_counts CHECK (
        correct_count >= 0 AND miss_count >= 0 AND max_combo >= 0
    ),
    CONSTRAINT chk_game_results_completion CHECK (completion_rate BETWEEN 0 AND 100),
    CONSTRAINT chk_game_results_play_time CHECK (play_time_ms >= 0)
);

CREATE TABLE line_judgements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_result_id UUID NOT NULL REFERENCES game_results(id) ON DELETE CASCADE,
    lyric_line_id UUID NOT NULL REFERENCES lyric_lines(id),
    judgement VARCHAR(20) NOT NULL,
    submitted_at_ms INTEGER,
    CONSTRAINT uk_line_judgements_result_line UNIQUE (game_result_id, lyric_line_id),
    CONSTRAINT chk_line_judgements_type CHECK (judgement IN ('CORRECT', 'WRONG', 'TIMEOUT')),
    CONSTRAINT chk_line_judgements_time CHECK (submitted_at_ms IS NULL OR submitted_at_ms >= 0)
);

CREATE TABLE content_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES users(id),
    song_id UUID NOT NULL REFERENCES songs(id),
    reason VARCHAR(30) NOT NULL,
    detail VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ,
    CONSTRAINT chk_content_reports_reason CHECK (
        reason IN ('COPYRIGHT', 'INAPPROPRIATE', 'BROKEN_VIDEO', 'OTHER')
    ),
    CONSTRAINT chk_content_reports_status CHECK (status IN ('OPEN', 'RESOLVED', 'REJECTED'))
);

CREATE INDEX idx_songs_status_published_at ON songs(status, published_at DESC);
CREATE INDEX idx_songs_normalized_name ON songs(normalized_title, normalized_artist);
CREATE INDEX idx_songs_youtube_video_id ON songs(youtube_video_id);
CREATE INDEX idx_game_results_user_played_at ON game_results(user_id, played_at DESC);
CREATE INDEX idx_content_reports_status_created_at ON content_reports(status, created_at);

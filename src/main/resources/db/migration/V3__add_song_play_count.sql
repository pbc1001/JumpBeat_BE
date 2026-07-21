ALTER TABLE songs
    ADD COLUMN play_count BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_songs_status_play_count
    ON songs(status, play_count DESC, published_at DESC);

CREATE INDEX idx_game_results_song_ranking
    ON game_results(song_id, score DESC, accuracy DESC, play_time_ms ASC, played_at ASC);

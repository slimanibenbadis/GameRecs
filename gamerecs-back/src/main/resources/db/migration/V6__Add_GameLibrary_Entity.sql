-- Create the game_libraries table to represent the GameLibrary entity
CREATE TABLE game_libraries (
    library_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_game_library_user
        FOREIGN KEY (user_id) 
        REFERENCES users(user_id)
        ON DELETE CASCADE
);

-- Create the join table to represent the many-to-many relationship between GameLibrary and Game
CREATE TABLE library_games (
    library_id BIGINT NOT NULL,
    game_id BIGINT NOT NULL,
    PRIMARY KEY (library_id, game_id),
    CONSTRAINT fk_library_games_library
        FOREIGN KEY (library_id) 
        REFERENCES game_libraries(library_id),
    CONSTRAINT fk_library_games_game
        FOREIGN KEY (game_id) 
        REFERENCES games(game_id)
);

-- Add indexes for better performance
CREATE INDEX idx_library_games_library_id ON library_games(library_id);
CREATE INDEX idx_library_games_game_id ON library_games(game_id); 

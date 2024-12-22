-- Users table
CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    profile_picture_url VARCHAR(255),
    bio TEXT,
    join_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

-- Games table
CREATE TABLE games (
    game_id BIGSERIAL PRIMARY KEY,
    igdb_id BIGINT NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    release_date DATE,
    cover_image_url VARCHAR(255),
    developer VARCHAR(255),
    publisher VARCHAR(255)
);

-- Genres table
CREATE TABLE genres (
    genre_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Platforms table
CREATE TABLE platforms (
    platform_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Game-Genre relationship
CREATE TABLE game_genres (
    game_id BIGINT REFERENCES games(game_id),
    genre_id BIGINT REFERENCES genres(genre_id),
    PRIMARY KEY (game_id, genre_id)
);

-- Game-Platform relationship
CREATE TABLE game_platforms (
    game_id BIGINT REFERENCES games(game_id),
    platform_id BIGINT REFERENCES platforms(platform_id),
    PRIMARY KEY (game_id, platform_id)
);

-- Game Library (user's games)
CREATE TABLE game_library (
    library_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(user_id),
    game_id BIGINT REFERENCES games(game_id),
    UNIQUE(user_id, game_id)
);

-- Ratings
CREATE TABLE ratings (
    rating_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(user_id),
    game_id BIGINT REFERENCES games(game_id),
    rating_value INTEGER NOT NULL CHECK (rating_value >= 0 AND rating_value <= 100),
    percentile_rank INTEGER CHECK (percentile_rank >= 0 AND percentile_rank <= 99),
    date_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, game_id)
);

-- Backlog Items
CREATE TABLE backlog_items (
    backlog_item_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(user_id),
    game_id BIGINT REFERENCES games(game_id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('To Play', 'In Progress', 'Completed', 'Abandoned')),
    UNIQUE(user_id, game_id)
);

-- Taste Compatibility Index
CREATE TABLE taste_compatibility_index (
    tci_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(user_id),
    similar_user_id BIGINT REFERENCES users(user_id),
    tci_value DECIMAL(7,4) NOT NULL CHECK (tci_value >= 0),
    date_calculated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, similar_user_id)
);

-- Probable Rating Indicator
CREATE TABLE probable_rating_indicator (
    pri_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(user_id),
    game_id BIGINT REFERENCES games(game_id),
    predicted_rating INTEGER NOT NULL CHECK (predicted_rating >= 0 AND predicted_rating <= 100),
    date_predicted TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, game_id)
);

-- Create indexes for better query performance
CREATE INDEX idx_ratings_user_id ON ratings(user_id);
CREATE INDEX idx_ratings_game_id ON ratings(game_id);
CREATE INDEX idx_game_library_user_id ON game_library(user_id);
CREATE INDEX idx_game_library_game_id ON game_library(game_id);
CREATE INDEX idx_backlog_items_user_id ON backlog_items(user_id);
CREATE INDEX idx_backlog_items_game_id ON backlog_items(game_id);
CREATE INDEX idx_tci_user_id ON taste_compatibility_index(user_id);
CREATE INDEX idx_tci_similar_user_id ON taste_compatibility_index(similar_user_id);
CREATE INDEX idx_pri_user_id ON probable_rating_indicator(user_id);
CREATE INDEX idx_pri_game_id ON probable_rating_indicator(game_id); 
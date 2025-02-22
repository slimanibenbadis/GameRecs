-- Add updated_at column to games table
ALTER TABLE games ADD COLUMN updated_at TIMESTAMP;

-- Create Publishers table
CREATE TABLE publishers (
    publisher_id BIGSERIAL PRIMARY KEY,
    igdb_company_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL
);

-- Create Developers table
CREATE TABLE developers (
    developer_id BIGSERIAL PRIMARY KEY,
    igdb_company_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL
);

-- Create join table for games and publishers
CREATE TABLE game_publishers (
    game_id BIGINT REFERENCES games(game_id) ON DELETE CASCADE,
    publisher_id BIGINT REFERENCES publishers(publisher_id) ON DELETE CASCADE,
    PRIMARY KEY (game_id, publisher_id)
);

-- Create join table for games and developers
CREATE TABLE game_developers (
    game_id BIGINT REFERENCES games(game_id) ON DELETE CASCADE,
    developer_id BIGINT REFERENCES developers(developer_id) ON DELETE CASCADE,
    PRIMARY KEY (game_id, developer_id)
);

-- Drop old columns that are now moved to separate tables
ALTER TABLE games DROP COLUMN developer;
ALTER TABLE games DROP COLUMN publisher;

-- Add indexes for better performance
CREATE INDEX idx_publishers_igdb_id ON publishers(igdb_company_id);
CREATE INDEX idx_developers_igdb_id ON developers(igdb_company_id);
CREATE INDEX idx_games_updated_at ON games(updated_at); 

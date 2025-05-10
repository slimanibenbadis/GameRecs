ALTER TABLE users ADD COLUMN steam_api_key VARCHAR(255);
ALTER TABLE users ADD COLUMN steam_profile_id VARCHAR(255);

-- Optional: ALTER TABLE users ADD CONSTRAINT uq_steam_profile_id UNIQUE (steam_profile_id); 

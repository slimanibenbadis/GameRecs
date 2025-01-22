-- Add Google ID column and make password_hash nullable
ALTER TABLE users
    ADD COLUMN google_id VARCHAR(255) UNIQUE,
    ALTER COLUMN password_hash DROP NOT NULL; 

-- Add email_verified column to users table
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Create index for email_verified column for better query performance
CREATE INDEX idx_users_email_verified ON users(email_verified); 

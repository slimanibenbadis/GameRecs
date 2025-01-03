-- Create verification_tokens table
CREATE TABLE verification_tokens (
    token_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT uk_verification_tokens_token UNIQUE (token)
);

-- Create index for token lookup
CREATE INDEX idx_verification_tokens_token ON verification_tokens(token);

-- Create index for user_id lookup
CREATE INDEX idx_verification_tokens_user_id ON verification_tokens(user_id); 

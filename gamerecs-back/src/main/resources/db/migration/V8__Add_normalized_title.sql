-- Add normalized_title column to games table
ALTER TABLE games ADD COLUMN normalized_title TEXT;

-- Update existing records with normalized titles
UPDATE games SET normalized_title = LOWER(REGEXP_REPLACE(REGEXP_REPLACE(TRANSLATE(title, 'áàâäãåæçéèêëíìîïñóòôöõøœúùûüýÿÁÀÂÄÃÅÆÇÉÈÊËÍÌÎÏÑÓÒÔÖÕØŒÚÙÛÜÝŸ', 'aaaaaaeceeeeiiiinoooooouuuuyyAAAAAAECEEEEIIIINOOOOOOUUUUYY'), '&', ' and '), '[^a-zA-Z0-9\s]', ''));

-- Create an index on normalized_title for faster searches
CREATE INDEX idx_games_normalized_title ON games(normalized_title); 

// GameDto and related interfaces for Gamer-Reco frontend

/**
 * Represents a video game with all relevant metadata and user-specific info.
 */
export interface GameDto {
  /** Unique game ID (primary key) */
  id: number;
  /** Game title (required) */
  title: string;
  /** Game description (may be empty) */
  description: string;
  /** Release date in ISO format (YYYY-MM-DD) */
  releaseDate: string;
  /** URL to the cover image */
  coverImageUrl: string;
  /** List of genres associated with the game */
  genres: Genre[];
  /** List of platforms the game is available on */
  platforms: Platform[];
  /** List of publishers */
  publishers: Publisher[];
  /** List of developers */
  developers: Developer[];
  /** The user's rating for this game (0-100), or null if not rated */
  userRating: number | null;
  /** The percentile rank of the user's rating (0-99), or null if not rated */
  percentileRank: number | null;
  /** Predicted rating based on similar users, or null if not available */
  predictedRating: number | null;
  /** User's backlog status for this game */
  backlogStatus: BacklogStatus | null;
}

/**
 * Genre entity (id and name only)
 */
export interface Genre {
  /** Unique genre ID */
  id: number;
  /** Genre name */
  name: string;
}

/**
 * Platform entity (id and name only)
 */
export interface Platform {
  /** Unique platform ID */
  id: number;
  /** Platform name */
  name: string;
}

/**
 * Publisher entity (id and name only)
 */
export interface Publisher {
  /** Unique publisher ID */
  id: number;
  /** Publisher name */
  name: string;
}

/**
 * Developer entity (id and name only)
 */
export interface Developer {
  /** Unique developer ID */
  id: number;
  /** Developer name */
  name: string;
}

/**
 * Allowed backlog status values for a game in the user's library
 */
export type BacklogStatus = 'To Play' | 'In Progress' | 'Completed' | 'Abandoned';

// --- Simple type test (not run at runtime, just for compile check) ---
// This block will not be included in production, but helps ensure type correctness.
const _testGame: GameDto = {
  id: 1,
  title: 'The Witcher 3: Wild Hunt',
  description: 'An open-world RPG',
  releaseDate: '2015-05-19',
  coverImageUrl: 'https://example.com/witcher3.jpg',
  genres: [{ id: 1, name: 'RPG' }],
  platforms: [{ id: 1, name: 'PC' }],
  publishers: [{ id: 1, name: 'CD Projekt' }],
  developers: [{ id: 1, name: 'CD Projekt Red' }],
  userRating: 90,
  percentileRank: 95,
  predictedRating: 88,
  backlogStatus: 'Completed',
}; 

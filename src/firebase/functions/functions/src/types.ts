import * as admin from "firebase-admin";

admin.initializeApp({
  databaseURL: "https://word-war-4-default-rtdb.asia-southeast1.firebasedatabase.app",
});

export {admin};

/**
 * Represents a single player's data within a live game.
 */
export interface Player {
  /**
   * The player's current status in the game.
   * @example "On", "Off", "Disconnected"
   */
  status: string;

  /**
   * The timestamp when the player joined the game.
   */
  joinedAt: number;
}

/**
 * Represents the entire data structure for a live game.
 */
export interface LiveGame {
  /**
   * An object where keys are player IDs and values are Player objects.
   */
  players: { [playerId: string]: Player };

  /**
   * The number of players required for this match.
   */
  matchSize: number;

  /**
   * The timestamp when the game was created.
   */
  createdAt: number;

  /**
   * A 2D array representing the game board's cell data.
   */
  cellData: string[][];

  /**
   * The ID of the current player whose turn it is.
   */
  currentPlayer: string;

  /**
   * The ID of the currently selected cell, if any.
   */
  selectedCell: string;

  /**
   * The timestamp of when the current turn started.
   */
  turnTimestamp: number;
}


// Define the structure of the nested 'data' object from the API response.
export interface GameData {
  _id: string;
  players: { [key: string]: { status: string; joinedAt: number } };
  matchSize: number;
  createdAt: string;
  currentPlayer: string;
  selectedCell: string;
  turnTimestamp: number;
  updatedAt: string;
}

// Define the overall structure of the API response.
export interface ApiResponse {
  status: number;
  data: GameData;
}

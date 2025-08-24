import * as admin from "firebase-admin";
import {onValueUpdated} from "firebase-functions/v2/database";
import {Player} from "./types";
import {getNextPlayerId} from "./utils";

export const onCellDataUpdated = onValueUpdated(
  {
    ref: "/live_games/{gameId}/cellData",
    region: "asia-southeast1",
  },
  async (event) => {
    const {gameId} = event.params;
    const db = admin.database();

    // Get a reference to the specific game
    const gameRef = db.ref(`/live_games/${gameId}`);

    // Fetch the entire game object to get all player information
    const gameSnapshot = await gameRef.once("value");
    const gameData = gameSnapshot.val();

    if (!gameData) {
      console.error(`Game with ID ${gameId} not found`);
      return;
    }

    const players = gameData.players as { [key: string]: Player };
    const currentPlayerId = gameData.currentPlayer;

    const nextPlayerId = getNextPlayerId(players, currentPlayerId);

    await gameRef.update({
      currentPlayer: nextPlayerId,
      turnTimestamp: Date.now(),
      selectedCell: "",
    });

    console.log(`Updated game ${gameId}: next player is ${nextPlayerId}`);
  }
);

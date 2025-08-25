import * as admin from "firebase-admin";
import {onCall} from "firebase-functions/v2/https";
import {Player} from "./types";
import {getNextPlayerId} from "./utils";

export const advanceTurn = onCall(async (request) => {
  const {gameId} = request.data;
  if (!gameId) {
    throw new Error("Game ID is required.");
  }

  const db = admin.database();
  const gameRef = db.ref(`/live_games/${gameId}`);

  // Use a transaction to ensure atomicity. Only one can succeed.
  let updateSuccessful = false;
  await gameRef.transaction((gameData) => {
    // This function is the "atomic" part. It runs on the server.
    if (!gameData) {
      console.error(`Game with ID ${gameId} not found`);
      return gameData; // Abort transaction
    }

    const now = Date.now();
    const thirtySecondsAgo = now - 30 * 1000;

    if (gameData.turnTimestamp > thirtySecondsAgo) {
      console.log(`Game ${gameId}: Turn has not yet expired. Aborting.`);
      return;
    }

    const players = gameData.players as { [key: string]: Player };
    const currentPlayerId = gameData.currentPlayer;

    gameData.currentPlayer = getNextPlayerId(players, currentPlayerId);
    gameData.turnTimestamp = now;
    gameData.phase = "EDIT";
    gameData.selectedCell = "";

    // This flag helps us know if the transaction was committed.
    updateSuccessful = true;

    return gameData;
  });

  if (updateSuccessful) {
    return {success: true};
  } else {
    return {success: false, reason: "Another client beat us to the update."};
  }
});

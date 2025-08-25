import * as admin from "firebase-admin";

export const getNextPlayerId = (
  players: {[key: string]: any}, currentPlayerId: string
): string => {
  // Sort players by their joinedAt timestamp to ensure a consistent turn order
  const sortedPlayers = Object.entries(players).sort(([, a], [, b]) => {
    return a.joinedAt - b.joinedAt;
  });

  const playerIds = sortedPlayers.map(([id]) => id);
  const currentIndex = playerIds.indexOf(currentPlayerId);

  // Determine the next player
  let nextPlayerId;
  if (currentIndex === -1 || currentIndex === playerIds.length - 1) {
    // If the current player isn't found or is the last one, loop back to the first player.
    nextPlayerId = playerIds[0];
  } else {
    // Otherwise, move to the next player in the sorted list.
    nextPlayerId = playerIds[currentIndex + 1];
  }

  return nextPlayerId;
};


export async function triggerAdvanceTurn(gameId: string) {
  if (!gameId) {
    throw new Error("Game ID is required.");
  }

  const db = admin.database();
  const gameRef = db.ref(`/live_games/${gameId}`);

  let updateSuccessful = false;

  await gameRef.transaction((gameData) => {
    if (!gameData) {
      console.error(`Game with ID ${gameId} not found`);
      return gameData;
    }

    const now = Date.now();

    const players = gameData.players as { [key: string]: any };
    const currentPlayerId = gameData.currentPlayer;

    gameData.currentPlayer = getNextPlayerId(players, currentPlayerId);
    gameData.turnTimestamp = now;
    gameData.phase = "EDIT";
    gameData.selectedCell = "";

    updateSuccessful = true;
    return gameData; // commit transaction
  });

  if (updateSuccessful) {
    return { success: true };
  } else {
    return { success: false, reason: "Another client beat us to the update." };
  }
}

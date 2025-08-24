import {Player} from "./types";


export const getNextPlayerId = (
  players: {[key: string]: Player}, currentPlayerId: string
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

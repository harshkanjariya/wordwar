import {onValueUpdated} from "firebase-functions/database";
import {admin, Player} from "./types";
import fetch from "node-fetch";

export const onVoteEndGame = onValueUpdated(
  {
    ref: "/live_games/{gameId}/voteEndGame",
    region: "asia-southeast1",
  },
  async (event) => {
    const {gameId} = event.params;
    const newVoteArray = event.data.after.val() as string[] | null;

    if (!newVoteArray || newVoteArray.length === 0) {
      return;
    }

    const db = admin.database();
    const gameRef = db.ref(`/live_games/${gameId}`);

    try {
      const gameSnapshot = await gameRef.once("value");
      const gameData = gameSnapshot.val();

      if (!gameData) {
        console.log(`[VOTE] Game ${gameId} not found. Aborting.`);
        return;
      }

      const players = gameData.players as Record<string, Player>;

      const onlinePlayers = Object.keys(players).filter(
        (player) => players[player].status === "Online"
      );

      const onlinePlayerCount = onlinePlayers.length;
      const voteCount = newVoteArray.filter((playerId) => onlinePlayers.includes(playerId)).length;

      if (voteCount >= onlinePlayerCount && onlinePlayerCount > 0) {
        const apiURL = "https://word-war-4.web.app/api";
        const apiKey = "5cdf2476-491a-4cc5-8ff2-ecd8767a7e23";

        try {
          const response = await fetch(`${apiURL}/game/end_game`, {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              "x-api-key": apiKey,
            },
            body: JSON.stringify({gameId: gameId}),
          });

          if (!response.ok) {
            const errorText = await response.text();
            // eslint-disable-next-line max-len
            console.error(`[VOTE] End game API call failed with status: ${response.status}, message: ${errorText}`);
            return;
          }
        } catch (error) {
          console.error(`[VOTE] Failed to call end game API for ${gameId}:`, error);
        }
      } else {
        console.log(`[VOTE] Game ${gameId}: Not enough votes to end game yet.`);
      }
    } catch (error) {
      console.error(`[VOTE] Error processing vote end game for ${gameId}:`, error);
    }
  }
);

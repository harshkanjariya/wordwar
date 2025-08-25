import {onValueUpdated} from "firebase-functions/database";
import {admin, Player} from "./types";
import fetch from "node-fetch";

export const onPlayerStatusChange = onValueUpdated(
  {
    ref: "/live_games/{gameId}/players/{userId}",
    region: "asia-southeast1",
  },
  async (event) => {
    const {gameId} = event.params;

    const db = admin.database();
    const gameRef = db.ref(`/live_games/${gameId}`);

    const gameSnapshot = await gameRef.once("value");
    const gameData = gameSnapshot.val();

    if (!gameData) {
      console.log(`[END] Game ${gameId} not found. Aborting.`);
      return;
    }

    const players = gameData.players as Record<string, Player>;
    const onlinePlayers = Object.values(players).filter(
      (player) => player.status === "Online"
    );

    if (onlinePlayers.length < 2) {
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
          console.error(`[ERROR] End game API call failed with status: ${response.status}, message: ${errorText}`);
          return;
        }
      } catch (error) {
        console.error(`[ERROR] Failed to call end game API for ${gameId}:`, error);
      }
    } else {
      console.log("[END] Enough players are online. Aborting.");
    }
  }
);

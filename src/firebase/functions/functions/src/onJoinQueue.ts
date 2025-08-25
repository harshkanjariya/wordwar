import {admin, ApiResponse, Player} from "./types";
import {onValueCreated} from "firebase-functions/database";

/**
 * Creates a new game via an external API.
 * @param {object} payload The game payload to send to the API.
 * @return {Promise<string>} The ID of the newly created game.
 */
async function createGameViaApi(payload: any): Promise<string> {
  const apiURL = "https://word-war-4.web.app/api";
  const apiKey = "5cdf2476-491a-4cc5-8ff2-ecd8767a7e23";

  if (!apiURL || !apiKey) {
    throw new Error("API_URL or API_KEY environment variables are not set.");
  }

  try {
    const response = await fetch(`${apiURL}/game/start_game`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-api-key": apiKey,
      },
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error(`API call failed with status: ${response.status}, message: ${errorText}`);
      throw new Error(`API call failed: ${response.status}`);
    }

    const result = await response.json() as ApiResponse;
    return result.data._id;
  } catch (error) {
    console.error("Failed to call secure API:", error);
    throw error;
  }
}

export const onJoinQueue = onValueCreated(
  {
    ref: "/matchmaking_queue/{queueSize}/{userId}",
    region: "asia-southeast1",
  },
  async (event) => {
    const {queueSize} = event.params;
    const db = admin.database();

    const parentRef = event.data.ref.parent;
    if (!parentRef) {
      console.error("No parent ref found for event");
      return;
    }

    const size = parseInt(queueSize as string, 10);

    const queueSnapshot = await parentRef.get();
    const data = queueSnapshot.val();

    if (!data) {
      console.warn("Queue is empty. Aborting match attempt.");
      return;
    }

    const ids = Object.keys(data);

    if (ids.length >= size) {
      const playersToMatch = ids.slice(0, size);

      const playersJson: Record<string, Player> = {};
      for (const playerId of playersToMatch) {
        playersJson[playerId] = {
          status: "Online",
          joinedAt: data[playerId]?.timestamp,
        };
      }

      const cellData: string[][] = Array.from({length: 10}, () => Array(10).fill(""));
      const gamePayload = {
        players: playersJson,
        matchSize: size,
        createdAt: Date.now(),
        cellData: cellData,
        currentPlayer: playersToMatch[0],
        phase: "EDIT",
        selectedCell: "",
        turnTimestamp: Date.now(),
      };

      let liveGameId: string;
      try {
        liveGameId = await createGameViaApi(gamePayload);
      } catch (error) {
        console.error("Matchmaking failed: API call to create game failed.");
        return;
      }

      const updatedQueue: Record<string, unknown> = {};
      for (const id of ids.slice(size)) {
        updatedQueue[id] = data[id];
      }

      await db.ref(`live_games/${liveGameId}`).set(gamePayload);
      await parentRef.set(updatedQueue);
    } else {
      console.log("Not enough players yet. Waiting for more to join.");
    }
  }
);

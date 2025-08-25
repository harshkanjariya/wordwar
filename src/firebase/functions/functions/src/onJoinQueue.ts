import * as admin from "firebase-admin";
import {onValueCreated} from "firebase-functions/v2/database";
import {ApiResponse, Player} from "./types";
import fetch from "node-fetch";

// eslint-disable-next-line require-jsdoc
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

    const transactionResult = await parentRef.transaction(async (currentQueueData) => {
      if (!currentQueueData) {
        console.warn("Queue is empty. Aborting transaction.");
        return;
      }

      const ids = Object.keys(currentQueueData);

      if (ids.length >= size) {
        console.log("Forming match inside transaction...", {currentQueueData});
        const playersToMatch = ids.slice(0, size);

        const playersJson: Record<string, Player> = {};
        for (const playerId of playersToMatch) {
          playersJson[playerId] = {
            status: "Online",
            joinedAt: currentQueueData[playerId]?.timestamp,
          };
        }

        const cellData: string[][] = Array.from({length: 10}, () => Array(10).fill(""));
        const gamePayload = {
          players: playersJson,
          matchSize: size,
          createdAt: Date.now(),
          cellData: cellData,
          currentPlayer: playersToMatch[0],
          selectedCell: "",
          turnTimestamp: Date.now(),
        };

        let liveGameId: string;
        try {
          liveGameId = await createGameViaApi(gamePayload);
        } catch (error) {
          console.error("Transaction failed: API call failed.");
          return;
        }

        const updatedQueue: Record<string, unknown> = {};
        for (const id of ids.slice(size)) {
          updatedQueue[id] = currentQueueData[id];
        }

        await db.ref(`live_games/${liveGameId}`).set(gamePayload);

        console.log("Transaction successful: Game created and queue updated.");
        return updatedQueue;
      } else {
        console.log("Not enough players yet. Aborting transaction.");
        return;
      }
    });

    if (transactionResult.committed) {
      console.log("Matchmaking transaction completed successfully.");
    } else {
      console.log("Matchmaking transaction aborted or failed.");
    }
  }
);

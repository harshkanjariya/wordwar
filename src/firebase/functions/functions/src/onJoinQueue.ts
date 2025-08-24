import * as admin from "firebase-admin";
import {onValueCreated} from "firebase-functions/v2/database";
import {ApiResponse, Player} from "./types";
import fetch from "node-fetch";

// Initialize Firebase Admin SDK
admin.initializeApp();

// --- THE CORRECTED TRIGGER FUNCTION ---
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

    // Get the latest state of the queue directly
    const snapshot = await parentRef.once("value");
    const currentQueue = snapshot.val();

    if (!currentQueue) {
      console.warn("Queue is empty. Aborting function.");
      return;
    }

    const size = parseInt(queueSize as string, 10);
    const ids = Object.keys(currentQueue);

    if (ids.length >= size) {
      // Pick first `size` players
      const playersToMatch = ids.slice(0, size);

      // Keep only remaining players in queue
      const updated: Record<string, unknown> = {};
      for (const id of ids.slice(size)) {
        updated[id] = currentQueue[id];
      }

      console.log("Formed match", {playersToMatch, remaining: Object.keys(updated)});

      const playersJson: Record<string, Player> = {};
      for (const playerId of playersToMatch) {
        playersJson[playerId] = {
          status: "Online",
          joinedAt: currentQueue[playerId]?.timestamp,
        };
      }

      // Create the 10x10 array with empty strings
      const cellData: string[][] = Array.from({length: 10}, () => Array(10).fill(""));

      // --- START OF API CALL LOGIC ---
      const apiURL = "https://word-war-4.web.app/api";
      const apiKey = "5cdf2476-491a-4cc5-8ff2-ecd8767a7e23";

      if (!apiURL || !apiKey) {
        console.error("API_URL or API_KEY environment variables are not set.");
        return;
      }

      const payload = {
        players: playersJson,
        matchSize: size,
        createdAt: Date.now(),
        cellData: cellData,
        currentPlayer: playersToMatch[0],
        selectedCell: "",
        turnTimestamp: Date.now(),
      };

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
          return;
        }

        const result = await response.json() as ApiResponse;
        const liveGameId = result.data._id;

        await db.ref(`live_games/${liveGameId}`).set(payload);

        await parentRef.set(updated);
      } catch (error) {
        console.error("Failed to call secure API:", error);
      }
    } else {
      console.log("Not enough players yet. Waiting...");
    }
  }
);

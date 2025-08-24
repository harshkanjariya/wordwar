import * as admin from "firebase-admin";
import {onValueCreated} from "firebase-functions/v2/database";
import {Player} from "./types";

admin.initializeApp();

export const onJoinQueue = onValueCreated(
  {
    ref: "/matchmaking_queues/{queueSize}/{userId}",
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

    await parentRef.transaction((currentQueue) => {
      if (!currentQueue) {
        return currentQueue; // no players
      }

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
            status: "On",
            joinedAt: currentQueue[playerId]?.timestamp,
          };
        }

        // Create the 10x10 array with empty strings
        const cellData: string[][] = Array.from({length: 10}, () => Array(10).fill(""));

        db.ref("live_games").push({
          players: playersJson,
          matchSize: size,
          createdAt: Date.now(),
          cellData: cellData,
          currentPlayer: playersToMatch[0],
          selectedCell: "",
          turnTimestamp: Date.now(),
        });

        return updated; // This commits the change to the queue
      }

      // Not enough players yet
      return currentQueue;
    });
  }
);

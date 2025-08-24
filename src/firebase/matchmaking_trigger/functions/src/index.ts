import * as admin from "firebase-admin";
import {onValueCreated} from "firebase-functions/v2/database";

admin.initializeApp();

export const onJoinQueue = onValueCreated(
  {ref: "/matchmaking_queues/{queueSize}/{userId}"},
  async (event) => {
    const {queueSize, userId} = event.params;
    const db = admin.database();

    console.log("Triggered onJoinQueue", {queueSize, userId});

    const parentRef = event.data.ref.parent; // /matchmaking_queues/{queueSize}
    if (!parentRef) {
      console.error("No parent ref found for event");
      return;
    }

    const size = parseInt(queueSize as string, 10);
    let playersToMatch: string[] = [];

    // 🔒 Transaction to ensure only one function instance succeeds
    const result = await parentRef.transaction((currentQueue) => {
      console.log("Running transaction with currentQueue:", currentQueue);

      if (!currentQueue) {
        return currentQueue; // no players
      }

      const ids = Object.keys(currentQueue);
      if (ids.length >= size) {
        // Pick first `size` players
        playersToMatch = ids.slice(0, size);

        // Keep only remaining players in queue
        const updated: Record<string, unknown> = {};
        for (const id of ids.slice(size)) {
          updated[id] = currentQueue[id];
        }

        console.log("Formed match", {playersToMatch, remaining: Object.keys(updated)});
        return updated;
      }

      // Not enough players yet
      return currentQueue;
    });

    console.log("Transaction result:", {
      committed: result.committed,
      snapshot: result.snapshot?.val(),
    });

    // If the transaction succeeded AND we selected players
    if (result.committed && playersToMatch.length) {
      console.log("Pushing live game", {playersToMatch, size});

      await db.ref("live_games").push({
        players: playersToMatch,
        matchSize: size,
        createdAt: Date.now(),
      });
    } else {
      console.log("Not enough players or transaction not committed");
    }
  }
);

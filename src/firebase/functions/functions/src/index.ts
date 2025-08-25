import * as admin from "firebase-admin";

admin.initializeApp({
  databaseURL: "https://word-war-4-default-rtdb.asia-southeast1.firebasedatabase.app",
});

export {onJoinQueue} from "./onJoinQueue";
export {advanceTurn} from "./nextPlayerTurn";
export {onPlayerStatusChange} from "./onPlayerStatusUpdate";

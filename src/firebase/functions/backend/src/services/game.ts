import {repositories} from "../db/repositories";
import {User} from "../types";
import {GameAction} from "../dtos/game";
import {ObjectId} from "mongodb";
import {FullDocument} from "../types/api";
import {GameHistory, LiveGame} from "../types/game";
import {ApiError} from "../bootstrap/errors";
import {firebaseDatabase} from "../utils/firebase";

export async function createLiveGame(body: any) {
  const session = await repositories.startTransaction();

  const joinedAt: Record<string, number> = {};

  Object.keys(body.players).forEach((player) => {
    joinedAt[player] = body.players[player].timestamp;
  })

  const payload: LiveGame = {
    players: Object.keys(body.players),
    joinedAt: joinedAt,
    createdAt: Date.now(),
    currentPlayer: body.currentPlayer,
    cellData: body.cellData,
    claimedWords: body.claimedWords || {},
  }
  try {
    const game = await repositories.live_games.create(payload, {session});

    await repositories.users.update({
      _id: {
        $in: Object.keys(body.players).map(o => ObjectId.createFromHexString(o)),
      },
    }, {
      currentGameId: game._id,
    }, {session});

    await session.commitTransaction();

    return game;
  } catch (error) {
    await session.abortTransaction();
    throw error;
  } finally {
    await session.endSession();
  }
}

export async function performGameAction(user: FullDocument<User>, body: GameAction) {
  if (!body.isValid()) {
    throw new ApiError("Invalid action", 400);
  }

  const userData = await repositories.users.findOne({filter: {_id: user._id}});
  if (!userData?.currentGameId) {
    throw new ApiError("You're not in any active game!", 400);
  }

  const gameRef = firebaseDatabase.ref(`live_games/${userData.currentGameId}`);

  const snapshot = await gameRef.once('value');
  const liveGame = snapshot.val();

  if (liveGame.currentPlayer !== user._id) {
    throw new ApiError("It's not your turn", 400);
  }

  if (body.character) {
    const cellData = liveGame.cellData;
    cellData[body.row as any][body.col as any] = body.character;
    await gameRef.child("cellData/" + body.row + "/" + body.col).set(body.character);
  }

  return {};
}

export async function getCurrentGameInfo(user: FullDocument<User>) {
  const userData = await repositories.users.findOne({filter: {_id: user._id}});
  if (!userData?.currentGameId) {
    return {
      currentGameId: null,
      liveGameData: null,
    };
  }

  const gameRef = firebaseDatabase.ref(`live_games/${userData.currentGameId}`);

  try {
    const snapshot = await gameRef.once('value');
    const liveGameData = snapshot.val();

    const dbGameData = await repositories.live_games.findOne({filter: {_id: userData.currentGameId}});

    return {
      currentGameId: userData.currentGameId,
      gameData: {
        ...dbGameData,
        ...liveGameData,
      },
    };
  } catch (error) {
    console.error(`Failed to fetch game data for ${userData.currentGameId}:`, error);
    return {
      currentGameId: userData.currentGameId,
      liveGameData: null,
      error: "Failed to fetch live game data."
    };
  }
}

export async function quitGame(user: FullDocument<User>) {
  const userData = await repositories.users.findOne({filter: {_id: user._id}});
  if (!userData?.currentGameId) {
    throw new ApiError("You're not in any active game!", 400);
  }

  const gameRef = firebaseDatabase.ref(`live_games/${userData.currentGameId}`);

  await repositories.users.updateRaw({
    _id: user._id,
  }, {
    $unset: {
      currentGameId: true,
    }
  });

  await gameRef.child("players/" + user._id + "/status").set("quit");

  return true;
}


export async function endGame(gameId: string) {
  const liveGame = await repositories.live_games.findOne({filter: {_id: ObjectId.createFromHexString(gameId)}});

  if (!liveGame) {
    throw new ApiError("Live game not found", 404);
  }

  const gameRef = firebaseDatabase.ref(`live_games/${gameId}`);
  const firebaseSnapshot = await gameRef.once('value');
  const liveFirebaseData = firebaseSnapshot.val();
  if (!liveFirebaseData) {
    throw new ApiError("Live game data not found in Firebase", 404);
  }


  const session = await repositories.startTransaction();

  try {
    const leftAt: Record<string, number> = {};
    liveGame.players.forEach(playerId => {
      leftAt[playerId] = Date.now();
    });

    const gameHistoryPayload: GameHistory = {
      players: liveGame.players,
      joinedAt: liveGame.joinedAt,
      leftAt: leftAt,
      startedAt: liveGame.createdAt,
      endedAt: Date.now(),
      cellData: liveGame.cellData,
      claimedWords: liveGame.claimedWords || {},
    };

    await repositories.game_history.create(gameHistoryPayload, {session});

    await repositories.users.updateRaw(
      {_id: {$in: liveGame.players.map(p => ObjectId.createFromHexString(p))}},
      {$unset: {currentGameId: true}},
      {session}
    );

    await repositories.live_games.deleteOne({filter: {_id: ObjectId.createFromHexString(gameId)}}, {session});

    await gameRef.remove();

    await session.commitTransaction();

    return {success: true};

  } catch (error) {
    await session.abortTransaction();
    throw error;
  } finally {
    await session.endSession();
  }
}

import {repositories} from "../db/repositories";
import {User} from "../types";
import {GameAction} from "../dtos/game";
import {ObjectId} from "mongodb";
import {FullDocument} from "../types/api";
import {GameHistory, LiveGame} from "../types/game";
import {ApiError} from "../bootstrap/errors";
import {firebaseDatabase} from "../utils/firebase";
import {triggerAdvanceTurn} from "../external_apis/advance_turn";
import {
  buildWordFromCells,
  validateCellsFilled,
  validateEmptyCell,
  validateLinearSelection, validateWord, validateWordNotClaimed
} from "../utils/game_action_validation";

export async function createLiveGame(body: any) {
  const session = await repositories.startTransaction();

  const joinedAt: Record<string, number> = {};

  Object.keys(body.players).forEach((player) => {
    joinedAt[player] = body.players[player].timestamp;
  })

  const userIds = Object.keys(body.players);

  const usersWithActiveGame = await repositories.users.findAll({
    filter: {
      _id: {$in: userIds.map(o => ObjectId.createFromHexString(o))},
      // @ts-ignore
      currentGameId: {$ne: null},
    }
  });

  if (usersWithActiveGame.length > 0) {
    throw new ApiError("Some users are already in game", 400);
  }

  const payload: LiveGame = {
    players: userIds,
    joinedAt: joinedAt,
    createdAt: Date.now(),
    currentPlayer: body.currentPlayer,
    cellData: body.cellData,
    claimedWords: body.claimedWords || {},
  }
  try {
    const game = await repositories.live_games.create(payload, {session});

    const userIds = Object.keys(body.players).map(o => ObjectId.createFromHexString(o));

    await repositories.users.updateMany({
      _id: {$in: userIds},
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
  if (!body?.isValid()) {
    throw new ApiError("Invalid action", 400);
  }

  const userData = await repositories.users.findOne({ filter: { _id: user._id } });
  if (!userData) {
    throw new ApiError("User not found", 401);
  }

  if (!userData.currentGameId) {
    throw new ApiError("You're not in any active game!", 400);
  }

  const gameRef = firebaseDatabase.ref(`live_games/${userData.currentGameId}`);
  const snapshot = await gameRef.once("value");
  const liveGame = snapshot.val();
  const dbGame = await repositories.live_games.findOne({ filter: {_id: userData.currentGameId }});

  if (!liveGame || !dbGame) {
    throw new ApiError("Game not found", 404);
  }

  if (liveGame.currentPlayer !== user._id.toString()) {
    throw new ApiError("It's not your turn", 400);
  }

  const phase = liveGame.phase || "EDIT";

  if (!liveGame.cellData || !Array.isArray(liveGame.cellData)) {
    const BOARD_ROWS = 10;
    const BOARD_COLS = 10;

    liveGame.cellData = Array.from({ length: BOARD_ROWS }, () =>
      Array.from({ length: BOARD_COLS }, () => null)
    );

    await gameRef.child("cellData").set(liveGame.cellData);
  }

  if (phase === "EDIT") {
    // Must provide character + row + col
    if (body.character == null || body.row == null || body.col == null) {
      throw new ApiError("You must provide row, col, and character in EDIT phase", 400);
    }

    const { row, col, character } = body;

    validateEmptyCell(liveGame, row, col);

    if (
      typeof row !== "number" ||
      typeof col !== "number" ||
      row < 0 ||
      col < 0 ||
      row >= liveGame.cellData.length ||
      col >= liveGame.cellData[row].length
    ) {
      throw new ApiError("Invalid cell coordinates", 400);
    }

    if (typeof character !== "string" || character.length !== 1) {
      throw new ApiError("Invalid character", 400);
    }

    liveGame.cellData[row][col] = character;
    await gameRef.child(`cellData/${row}/${col}`).set(character);

    const filledCount = liveGame.cellData
      .flat()
      .filter((c: any) => c != null && c !== "").length;

    if (filledCount >= 3) {
      await gameRef.child("phase").set("SELECT");
    } else {
      triggerAdvanceTurn(userData.currentGameId.toString());
    }

    await repositories.live_games.updateOne({
      _id: userData.currentGameId,
    }, {
      cellData: liveGame.cellData,
    });
  } else if (phase === "SELECT") {
    if (!body.claimedWords || !Array.isArray(body.claimedWords) || body.claimedWords.length === 0) {
      throw new ApiError("You must provide claimedWords in SELECTION phase", 400);
    }

    for (const word of body.claimedWords) {
      if (!word.word || !Array.isArray(word.cellCoordinates)) {
        throw new ApiError("Invalid claimed word format", 400);
      }
      // 1. Check line validity
      validateLinearSelection(word.cellCoordinates);

      // 2. Ensure all cells are filled
      validateCellsFilled(liveGame, word.cellCoordinates);

      // 3. Build word from grid
      const builtWord = buildWordFromCells(liveGame, word.cellCoordinates);

      if (builtWord.toLowerCase() !== word.word.toLowerCase()) {
        throw new ApiError("Claimed word does not match selected cells", 400);
      }

      // 4. Validate dictionary
      await validateWord(word.word);

      // 5. Ensure not already claimed
      validateWordNotClaimed(word.word, dbGame);
    }

    const existingClaimed = dbGame.claimedWords || {};
    existingClaimed[user._id?.toString()] = [...existingClaimed[user._id?.toString()] || [], ...body.claimedWords.map(o => o.word)];

    await repositories.live_games.updateOne({
      _id: userData.currentGameId,
    }, {
      claimedWords: existingClaimed,
    });
    triggerAdvanceTurn(userData.currentGameId.toString());
  } else {
    throw new ApiError(`Unsupported game phase: ${phase}`, 400);
  }

  return { success: true };
}

export async function getCurrentGameInfo(user: FullDocument<User>) {
  const userData = await repositories.users.findOne({ filter: { _id: user._id } });

  if (!userData) {
    throw new ApiError("User not found", 401);
  }

  let currentGameId = userData?.currentGameId ?? null;
  let liveGameData: any = null;

  if (!currentGameId) {
    const gamesSnapshot = await firebaseDatabase.ref("live_games").once("value");
    const allGames = gamesSnapshot.val() || {};

    for (const [gameId, game] of Object.entries<any>(allGames)) {
      if (game?.players?.includes?.(user._id.toString())) {
        currentGameId = ObjectId.createFromHexString(gameId);
        liveGameData = game;
        break;
      }
    }

    if (!currentGameId) {
      return {
        currentGameId: null,
        gameData: null,
      };
    }
  }

  try {
    const dbGameData = await repositories.live_games.findOne({ filter: { _id: userData.currentGameId } });
    if (!dbGameData && !liveGameData) {
      return {
        currentGameId: userData.currentGameId,
        gameData: null,
      };
    }

    const mergedGame = {
      ...dbGameData,
      ...liveGameData,
    };

    const players = await repositories.users.findAll({
      filter: { _id: { $in: mergedGame.players.map((p: string) => ObjectId.createFromHexString(p)) } },
    });

    const playersFormatted = mergedGame.players.map((playerId: string) => {
      const player = players.find((u: any) => u._id.toString() === playerId.toString());
      return {
        _id: playerId,
        name: player?.name ?? "Unknown",
        joinedAt: mergedGame.joinedAt?.[playerId] ?? null,
        claimedWords: mergedGame.claimedWords?.[playerId] ?? [],
      };
    });

    return {
      currentGameId: userData.currentGameId,
      gameData: {
        createdAt: mergedGame.createdAt,
        updatedAt: mergedGame.updatedAt,
        cellData: mergedGame.cellData,
        players: playersFormatted,
      },
    };
  } catch (error) {
    console.error(`Failed to fetch game data for ${userData.currentGameId}:`, error);
    throw new ApiError("Failed to fetch live game data.", 500);
  }
}

export async function getGameInfo(user: FullDocument<User>, gameId: string) {
  const userData = await repositories.users.findOne({filter: {_id: user._id}});

  const gameHistory = await repositories.game_history.findOne({filter: {_id: ObjectId.createFromHexString(gameId)}});

  if (!gameHistory) {
    throw new ApiError("Game not found", 404);
  }

  if (!gameHistory.players.includes(userData?._id?.toString() || "")) {
    throw new ApiError("User not in this game", 400);
  }

  return gameHistory;
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
  const liveGame = await repositories.live_games.findOne({
    filter: { _id: ObjectId.createFromHexString(gameId) },
  });

  if (!liveGame) {
    throw new ApiError("Live game not found", 404);
  }

  const gameRef = firebaseDatabase.ref(`live_games/${gameId}`);
  const firebaseSnapshot = await gameRef.once("value");
  const liveFirebaseData = firebaseSnapshot.val();
  if (!liveFirebaseData) {
    throw new ApiError("Live game data not found in Firebase", 404);
  }

  const session = await repositories.startTransaction();

  try {
    const leftAt: Record<string, number> = {};
    liveGame.players.forEach((playerId) => {
      leftAt[playerId] = Date.now();
    });

    const gameHistoryPayload: GameHistory = {
      // @ts-ignore
      _id: liveGame._id,
      players: liveGame.players,
      joinedAt: liveGame.joinedAt,
      leftAt: leftAt,
      startedAt: liveGame.createdAt,
      endedAt: Date.now(),
      cellData: liveGame.cellData,
      claimedWords: liveGame.claimedWords || {},
    };

    await repositories.game_history.updateOne(
      { filter: { _id: liveGame._id } },
      gameHistoryPayload,
      { upsert: true, session }
    );

    await repositories.users.updateRaw(
      { _id: { $in: liveGame.players.map((p) => ObjectId.createFromHexString(p)) } },
      { $unset: { currentGameId: true } },
      { session }
    );

    const result = await repositories.live_games.deleteOne(
      { _id: ObjectId.createFromHexString(gameId) },
      { session }
    );

    await gameRef.remove();

    await session.commitTransaction();

    return { success: true };
  } catch (error) {
    await session.abortTransaction();
    throw error;
  } finally {
    await session.endSession();
  }
}

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
  validateLinearSelection,
  validateWord,
  validateWordNotClaimed
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

  // Step 1: Fetch Firebase and DB entries in parallel with proper filtering
  const [firebaseGamesSnapshot, dbGames] = await Promise.all([
    // Firebase: Get all games and filter in memory (Firebase doesn't support $ne queries easily)
    (async () => {
      return await firebaseDatabase.ref("live_games").once("value");
    })(),
    // MongoDB: Query with nested field filter for non-QUIT status
    (async () => {
      return await repositories.live_games.findAll({
        filter: {
          [`players.${user._id}.status`]: {$ne: "QUIT"}
        }
      });
    })()
  ]);
  
  const allFirebaseGames = firebaseGamesSnapshot.val() || {};

  // Filter Firebase games where player exists and status is not QUIT
  const firebaseUserGames = Object.entries(allFirebaseGames)
    .filter(([gameId, game]: [string, any]) => {
      const playerData = game?.players?.[user._id.toString()];
      return playerData && playerData.status !== "QUIT";
    })
    .map(([gameId, game]: [string, any]) => ({
      gameId,
      gameData: game,
      createdAt: game.createdAt || 0
    }));

  // DB games are already filtered by the query, just map them
  const dbUserGames = dbGames.map((game: any) => ({
    gameId: game._id.toString(),
    gameData: game,
    createdAt: game.createdAt || 0
  }));

  // Step 4: Handle different scenarios
  const firebaseGameIds = new Set(firebaseUserGames.map(g => g.gameId));
  const dbGameIds = new Set(dbUserGames.map(g => g.gameId));

  // Entries in DB but not in Firebase - move to game history
  const dbOnlyGames = dbUserGames.filter(g => !firebaseGameIds.has(g.gameId));

  for (const game of dbOnlyGames) {
    await moveGameToHistory(game.gameId, game.gameData);
  }

  // Entries in Firebase but not in DB - delete from Firebase
  const firebaseOnlyGames = firebaseUserGames.filter(g => !dbGameIds.has(g.gameId));

  for (const game of firebaseOnlyGames) {
    await firebaseDatabase.ref(`live_games/${game.gameId}`).remove();
  }
  
  // Step 5: Handle common games (in both Firebase and DB)
  const commonGames = firebaseUserGames.filter(g => dbGameIds.has(g.gameId));

  if (commonGames.length === 0) {
    // No active games found
    return {
      currentGameId: null,
      gameData: null,
    };
  } else if (commonGames.length === 1) {
    // Single active game - update user profile and return
    const game = commonGames[0];
    
    await repositories.users.updateRaw(
      { _id: user._id },
      { $set: { currentGameId: ObjectId.createFromHexString(game.gameId) } }
    );
    return await getGameDataForResponse(game.gameId, game.gameData);
  } else {
    // Multiple active games - keep most recent, set others to QUIT
    const sortedGames = commonGames.sort((a, b) => b.createdAt - a.createdAt);
    const mostRecentGame = sortedGames[0];
    const gamesToQuit = sortedGames.slice(1);

    await repositories.users.updateRaw(
      { _id: user._id },
      { $set: { currentGameId: ObjectId.createFromHexString(mostRecentGame.gameId) } }
    );
    // Set other games to QUIT status
    for (const game of gamesToQuit) {
      const quitStartTime = Date.now();
      await setGameToQuitStatus(game.gameId, user._id.toString());
    }

    return await getGameDataForResponse(mostRecentGame.gameId, mostRecentGame.gameData);
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

  const players = await repositories.users.findAll({
    filter: { _id: { $in: gameHistory.players.map((p) => ObjectId.createFromHexString(p)) } },
  });

  const playerNames: Record<string, string> = {};
  players.forEach((player: any) => {
    playerNames[player._id.toString()] = player.name || "Unknown";
  });

  return {
    ...gameHistory,
    playerNames: playerNames
  };
}

export async function quitGame(user: FullDocument<User>) {
  const userData = await repositories.users.findOne({filter: {_id: user._id}});
  
  // If user has currentGameId, use the existing logic
  if (userData?.currentGameId) {
    const gameRef = firebaseDatabase.ref(`live_games/${userData.currentGameId}`);

    // Update user's currentGameId to null
    await repositories.users.updateRaw({
      _id: user._id,
    }, {
      $unset: {
        currentGameId: true,
      }
    });

    // Set player status to "QUIT" in Firebase
    await gameRef.child(`players/${user._id}/status`).set("QUIT");

    // Also update the live_games repository to keep it in sync
    try {
      await repositories.live_games.updateRaw(
        { _id: userData.currentGameId },
        { 
          $set: { 
            [`players.${user._id}.status`]: "QUIT",
            updatedAt: Date.now()
          } 
        }
      );
    } catch (error) {
      console.error(`Failed to update live_games repository for user ${user._id} quit:`, error);
      // Don't throw error here as Firebase update succeeded
    }

    return true;
  }

  // Just ensure user's currentGameId is null (in case it wasn't properly cleared)
  await repositories.users.updateRaw(
    { _id: user._id },
    { $unset: { currentGameId: true } }
  );

  // Trigger background cleanup without waiting for it (non-blocking)
  cleanupOrphanedEntries(user._id.toString()).catch(error => 
    console.error(`Background cleanup failed for user ${user._id}:`, error)
  );

  return true;
}

// Background cleanup function for orphaned entries (non-blocking)
export async function cleanupOrphanedEntries(userId: string) {
  try {
    const [firebaseGamesSnapshot, dbGames] = await Promise.all([
      firebaseDatabase.ref("live_games").once("value"),
      repositories.live_games.findAll({
        filter: { 
          [`players.${userId}.status`]: { $ne: "QUIT" }
        }
      })
    ]);

    const allFirebaseGames = firebaseGamesSnapshot.val() || {};
    
    // Find Firebase games where user exists and status is not QUIT
    const firebaseUserGames = Object.entries(allFirebaseGames)
      .filter(([gameId, game]: [string, any]) => {
        const playerData = game?.players?.[userId];
        return playerData && playerData.status !== "QUIT";
      })
      .map(([gameId, game]: [string, any]) => ({
        gameId,
        gameData: game
      }));

    // Find DB games where user exists and status is not QUIT
    const dbUserGames = dbGames.map((game: any) => ({
      gameId: game._id.toString(),
      gameData: game
    }));

    // Set all orphaned entries to QUIT status (non-blocking)
    const promises = [];

    // Set Firebase orphaned entries to QUIT
    for (const game of firebaseUserGames) {
      const gameRef = firebaseDatabase.ref(`live_games/${game.gameId}`);
      promises.push(
        gameRef.child(`players/${userId}/status`).set("QUIT")
          .catch(error => console.error(`Failed to set Firebase game ${game.gameId} to QUIT:`, error))
      );
    }

    // Set DB orphaned entries to QUIT
    for (const game of dbUserGames) {
      promises.push(
        repositories.live_games.updateRaw(
          { _id: ObjectId.createFromHexString(game.gameId) },
          { 
            $set: { 
              [`players.${userId}.status`]: "QUIT",
              updatedAt: Date.now()
            } 
          }
        ).catch(error => console.error(`Failed to set DB game ${game.gameId} to QUIT:`, error))
      );
    }

    // Wait for all updates to complete
    await Promise.all(promises);
  } catch (error) {
    console.error(`Background cleanup failed for user ${userId}:`, error);
  }
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

// Helper function to move a game to history (based on endGame logic)
async function moveGameToHistory(gameId: string, gameData: any) {
  const session = await repositories.startTransaction();
  
  try {
    const leftAt: Record<string, number> = {};
    Object.keys(gameData.players || {}).forEach((playerId) => {
      leftAt[playerId] = Date.now();
    });

    const gameHistoryPayload: GameHistory = {
      // @ts-ignore
      _id: ObjectId.createFromHexString(gameId),
      players: Object.keys(gameData.players || {}),
      joinedAt: gameData.joinedAt || {},
      leftAt: leftAt,
      startedAt: gameData.createdAt || Date.now(),
      endedAt: Date.now(),
      cellData: gameData.cellData || [],
      claimedWords: gameData.claimedWords || {},
    };

    await repositories.game_history.updateOne(
      { filter: { _id: ObjectId.createFromHexString(gameId) } },
      gameHistoryPayload,
      { upsert: true, session }
    );

    await repositories.users.updateRaw(
      { _id: { $in: Object.keys(gameData.players || {}).map((p) => ObjectId.createFromHexString(p)) } },
      { $unset: { currentGameId: true } },
      { session }
    );

    await repositories.live_games.deleteOne(
      { _id: ObjectId.createFromHexString(gameId) },
      { session }
    );

    await session.commitTransaction();
  } catch (error) {
    await session.abortTransaction();
    console.error(`Failed to move game ${gameId} to history:`, error);
    throw error;
  } finally {
    await session.endSession();
  }
}

// Helper function to set a game to QUIT status in both Firebase and DB
async function setGameToQuitStatus(gameId: string, userId: string) {
  try {
    // Update Firebase
    const gameRef = firebaseDatabase.ref(`live_games/${gameId}`);
    await gameRef.child(`players/${userId}/status`).set("QUIT");

    // Update DB
    await repositories.live_games.updateRaw(
      { _id: ObjectId.createFromHexString(gameId) },
      { 
        $set: { 
          [`players.${userId}.status`]: "QUIT",
          updatedAt: Date.now()
        } 
      }
    );
  } catch (error) {
    console.error(`Failed to set game ${gameId} to QUIT status for user ${userId}:`, error);
    throw error;
  }
}

// Helper function to get formatted game data for response
async function getGameDataForResponse(gameId: string, gameData: any) {
  try {
    const players = await repositories.users.findAll({
      filter: { _id: { $in: Object.keys(gameData.players || {}).map((p: string) => ObjectId.createFromHexString(p)) } },
    });
    const playersFormatted = Object.keys(gameData.players || {}).map((playerId: string) => {
      const player = players.find((u: any) => u._id.toString() === playerId.toString());
      return {
        _id: playerId,
        name: player?.name ?? "Unknown",
        joinedAt: gameData.joinedAt?.[playerId] ?? null,
        claimedWords: gameData.claimedWords?.[playerId] ?? [],
      };
    });
    const result = {
      currentGameId: gameId,
      gameData: {
        createdAt: gameData.createdAt,
        updatedAt: gameData.updatedAt,
        cellData: gameData.cellData,
        players: playersFormatted,
      },
    };
    return result;
  } catch (error) {
    console.error(`Failed to format game data for ${gameId}:`, error);
    throw new ApiError("Failed to fetch live game data.", 500);
  }
}

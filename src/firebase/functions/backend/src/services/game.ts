import {repositories} from "../db/repositories";
import {User} from "../types";
import {GameAction} from "../dtos/game";

export function createLiveGame(body: any) {
  return repositories.live_games.create(body);
}

export async function performGameAction(user: User, body: GameAction) {
  return {};
}
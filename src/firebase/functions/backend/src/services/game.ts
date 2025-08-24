import {repositories} from "../db/repositories";

export function createLiveGame(body: any) {
  return repositories.live_games.create(body);
}
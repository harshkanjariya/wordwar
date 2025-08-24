import {BaseRepository} from "./base-repository";
import {getMongoClient} from "./database";
import {ClientSession} from "mongodb";
import {Configuration} from "../types/configuration";
import {ApiCallLog, WebhookLog} from "../types/logs";
import {Permission, Role, User} from "../types";
import {GameHistory, LiveGame} from "../types/game";

type RepoToType = {
  users: User;

  live_games: LiveGame;
  game_history: GameHistory;

  roles: Role;
  permissions: Permission;

  configurations: Configuration;

  apiCallLogs: ApiCallLog;
  webhookLogs: WebhookLog;
};

export const repositories: {
  [key in keyof RepoToType]: BaseRepository<RepoToType[key]>;
} & { startTransaction: () => Promise<ClientSession> } = {
  startTransaction: async () => {
    const db = await getMongoClient();
    const session = db.startSession();
    session.startTransaction();
    return session;
  },
} as any;

export function initializeRepositories() {
  repositories["users"] = new BaseRepository<User>("users", [
    [{email: 1}, {unique: true}],
  ]);

  repositories["live_games"] = new BaseRepository<LiveGame>("live_games", []);
  repositories["game_history"] = new BaseRepository<GameHistory>("game_history", []);

  repositories["roles"] = new BaseRepository<Role>("roles", []);
  repositories["permissions"] = new BaseRepository<Permission>("permissions", [
    [{value: 1}, {unique: true}],
  ]);

  repositories["configurations"] = new BaseRepository<Configuration>("configurations", []);

  repositories["apiCallLogs"] = new BaseRepository<ApiCallLog>("apiCallLogs", []);
  repositories["webhookLogs"] = new BaseRepository<WebhookLog>("webhookLogs", []);
}

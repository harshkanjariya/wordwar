import {BaseRepository} from "./base-repository";
import {getMongoClient} from "./database";
import {ClientSession} from "mongodb";
import {Configuration} from "../types/configuration";
import {ApiCallLog, WebhookLog} from "../types/logs";
import {Permission, Role, User} from "../types";

type RepoToType = {
  users: User;

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

  repositories["roles"] = new BaseRepository<Role>("roles", []);
  repositories["permissions"] = new BaseRepository<Permission>("permissions", [
    [{value: 1}, {unique: true}],
  ]);

  repositories["configurations"] = new BaseRepository<Configuration>("configurations", []);

  repositories["apiCallLogs"] = new BaseRepository<ApiCallLog>("apiCallLogs", []);
  repositories["webhookLogs"] = new BaseRepository<WebhookLog>("webhookLogs", []);
}

import {connectDB} from "../../db/database";
import {initializeRepositories} from "../../db/repositories";
import redisManager from "../../cache/redis";

export const setupServices = async () => {
  await connectDB();
  initializeRepositories();
  await redisManager.waitForConnection();
};

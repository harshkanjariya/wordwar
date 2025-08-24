import { MongoClient, Db } from "mongodb";
import {config} from "../config";

let db: Db;
let client: MongoClient;
export const existingMongoCollections: string[] = [];

export const connectDB = async () => {
  if (db) return;
  const uri = config.mongoUri;
  const dbName = uri.split("/").pop() || "default";
  client = new MongoClient(uri);

  try {
    console.log(`Connecting to MongoDB at ${uri}`);
    await client.connect();
    db = client.db(dbName);
    const result = await db.listCollections().toArray();
    result.forEach((entry) => {
      existingMongoCollections.push(entry.name);
    });
    console.log(`MongoDB connected to database: ${db.databaseName}`);
  } catch (error) {
    console.error("Error connecting to MongoDB:", error);
    throw error;
  }
};

export const getDB = (): Db => {
  if (!db) {
    throw new Error("Database not initialized. Call connectDB first.");
  }
  return db;
};

export const getMongoClient = async () => {
  if (!db) {
    throw new Error("Database not initialized. Call connectDB first.");
  }
  return client;
}
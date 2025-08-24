import Redis from "ioredis";
import {config} from "../config";

class RedisManager {
  private client: Redis;
  public isConnected: boolean;
  private waitCallback: () => void;

  constructor(uri: string) {
    this.client = new Redis(uri);

    this.client.on("connect", () => {
      console.log("Redis connected");
      this.isConnected = true;
      if (this.waitCallback) {
        this.waitCallback();
      }
    });

    this.client.on("error", (err) => {
      console.error("Redis error:", err);
      this.isConnected = false;
      if (this.waitCallback) {
        this.waitCallback();
      }
    });
  }

  waitForConnection(): Promise<boolean> {
    return new Promise((resolve, reject) => {
      if (this.isConnected) return resolve(true);
      this.waitCallback = () => {
        resolve(this.isConnected);
      };
    });
  }

  // Set a key-value pair with optional expiry time in seconds
  async set(key: string, value: any, expiryInSeconds?: number): Promise<void> {
    if (expiryInSeconds) {
      await this.client.set(key, JSON.stringify(value), "EX", expiryInSeconds);
    } else {
      await this.client.set(key, JSON.stringify(value));
    }
  }

  // Modified get function
  async get<T>(
    key: string,
    expiryInSeconds?: number,
    isMiss?: (value: T | null) => boolean,
    onMiss?: () => Promise<T>
  ): Promise<T> {
    const cachedValue = await this.client.get(key);

    const cleanValue = ((cachedValue && JSON.parse(cachedValue)) || cachedValue);

    // If the value is missing or invalid, fetch and cache a fresh value
    if (isMiss && onMiss && isMiss(cleanValue)) {
      const newValue = await onMiss();
      if (newValue) {
        await this.set(key, newValue, expiryInSeconds);
      }
      return newValue;
    }

    // Return the parsed cached value
    return cleanValue as T;
  }

  // Delete a key
  async delete(key: string): Promise<number> {
    return this.client.del(key);
  }

  // Check if a key exists
  async exists(key: string): Promise<boolean> {
    const result = await this.client.exists(key);
    return result === 1;
  }

  // Increment a key's value
  async increment(key: string): Promise<number> {
    return this.client.incr(key);
  }

  // Decrement a key's value
  async decrement(key: string): Promise<number> {
    return this.client.decr(key);
  }

  // Push to a list
  async pushToList(key: string, value: string): Promise<number> {
    return this.client.rpush(key, value);
  }

  // Pop from a list
  async popFromList(key: string): Promise<string | null> {
    return this.client.lpop(key);
  }

  // Get all elements from a list
  async getList(key: string): Promise<string[]> {
    return this.client.lrange(key, 0, -1);
  }

  // Close the connection
  async disconnect(): Promise<void> {
    await this.client.quit();
    console.log("Redis connection closed");
  }
}

// Singleton instance
const redisManager = new RedisManager(config.redisUri);

export default redisManager;

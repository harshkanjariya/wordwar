import {
  ClientSession,
  Collection,
  Db,
  DeleteResult,
  Document,
  Filter,
  IndexSpecification,
  ObjectId,
  UpdateFilter
} from "mongodb";
import {existingMongoCollections, getDB} from "./database";
import {QueryOptions} from "../utils/mongo-types";
import {google} from "firebase-functions/protos/compiledFirestore";
import type = google.type;
import {FullDocument} from "../types/api";

export class BaseRepository<T extends Document> {
  private readonly collection: Collection<T>;

  constructor(collectionName: string, indexes: [IndexSpecification, { unique?: boolean }][]) {
    const db: Db = getDB();
    this.collection = db.collection<T>(collectionName);
    if (!existingMongoCollections.includes(collectionName)) {
      db.createCollection(collectionName);
    }
    if (indexes.length) this._createIndexes(indexes);
  }

  async create(
    doc: T & { _id?: ObjectId },
    options?: { session?: ClientSession },
  ): Promise<FullDocument<T>> {
    const now = new Date();
    const docWithTimestamps = {
      ...doc,
      createdAt: now,
      updatedAt: now,
    };
    const result = await this.collection.insertOne(docWithTimestamps as any, options);
    return {_id: result.insertedId, ...docWithTimestamps} as any as FullDocument<T>;
  }

  // Create multiple documents with timestamps
  async createMultiple(docs: T[]): Promise<FullDocument<T>[]> {
    const now = new Date();
    const docsWithTimestamps = docs.map(doc => ({
      ...doc,
      createdAt: now,
      updatedAt: now,
    }));
    const result = await this.collection.insertMany(docsWithTimestamps as any);
    return docsWithTimestamps.map((doc, index) => ({
      ...doc,
      _id: result.insertedIds[index]
    })) as any as FullDocument<T>[];
  }

  async updateOne(
    filter: Filter<FullDocument<T>>,
    updates: Partial<FullDocument<T>>,
    options?: { session?: ClientSession, upsert?: boolean },
  ): Promise<FullDocument<T> | null> {
    const now = new Date();
    return (await this.collection.findOneAndUpdate(
      filter as any,
      {$set: {...updates, updatedAt: now}},
      {
        returnDocument: "after",
        ...options,
      }
    )) as any as FullDocument<T>;
  }

  async updateMany(
    filter: Filter<FullDocument<T>>,
    updates: Partial<FullDocument<T>>,
    options?: { session?: ClientSession },
  ): Promise<FullDocument<T> | null> {
    const now = new Date();
    return (await this.collection.updateMany(
      filter as any,
      {$set: {...updates, updatedAt: now}},
      {
        ...options,
      }
    )) as any as FullDocument<T>;
  }

  async updateRaw(
    filter: Filter<FullDocument<T>>,
    updates: UpdateFilter<T>,
    options?: { session?: ClientSession },
  ): Promise<FullDocument<T> | null> {
    const now = new Date();
    return (await this.collection.updateMany(
      filter as any,
      updates,
      {...options}
    )) as any as FullDocument<T>;
  }

  async findOne(options: QueryOptions<FullDocument<T>>): Promise<FullDocument<T> | null> {
    const {filter, project} = options;
    return await this.collection.findOne((filter || {}) as any, {
      projection: project,
    }) as any;
  }

  async findAll(options: QueryOptions<T>): Promise<FullDocument<T>[]>;
  async findAll(options: QueryOptions<T>, returnAsMap: false): Promise<FullDocument<T>[]>;
  async findAll(options: QueryOptions<T>, returnAsMap: true): Promise<Map<string, FullDocument<T>>>;
  async findAll(
    options: QueryOptions<T>,
    returnAsMap: boolean = false
  ): Promise<FullDocument<T>[] | Map<string, FullDocument<T>>> {
    const { filter = {}, project, sort } = options;
    const skip = typeof options.skip !== "number" ? parseInt(options.skip + '') || 0 : options.skip;
    const limit = typeof options.limit !== "number" ? parseInt(options.limit + '') || 0 : options.limit

    const query = this.collection.find(filter).skip(skip).limit(limit);

    if (project) query.project(project);
    if (sort) query.sort(sort as any);

    const documents = await query.toArray() as any[];

    if (returnAsMap) {
      return new Map(documents.map(doc => [doc._id.toString(), doc]));
    }

    return documents;
  }

  // Aggregate documents
  async aggregate(pipeline: Document[], options?: { allowDiskUse?: boolean }): Promise<FullDocument<T>[]> {
    const cursor = this.collection.aggregate<FullDocument<T>>(pipeline, options);
    return cursor.toArray();
  }

  // Delete a document by ID
  async deleteOne(filter: Filter<T>, options?: { session?: ClientSession }): Promise<DeleteResult> {
    return await this.collection.deleteOne(filter, options);
  }

  // Delete multiple documents
  async deleteMultiple(filter: Filter<T>, options?: { session?: ClientSession }): Promise<void> {
    await this.collection.deleteMany(filter, options);
  }

  // Count documents
  async count(filter?: Filter<T>): Promise<number> {
    return await this.collection.countDocuments(filter || {});
  }

  // Create indexes
  async _createIndexes(indexes: [IndexSpecification, { name?: string, unique?: boolean }][]) {
    let existingIndexes: any[] = [];
    try {
      existingIndexes = await this.collection.indexes();
    } catch {
    }

    const indexesToCreate = indexes.filter(([key, options]) => {
      const name = options?.name || Object.keys(key).join("_1_") + "_1";
      return !existingIndexes.some(idx => idx.name === name);
    });

    await Promise.all(
      indexesToCreate.map(([key, options]) =>
        this.collection
          .createIndex(key, options)
          .then(() => console.log(`Index created for ${this.collection.collectionName}:`, key))
          .catch(error => console.error(`Error creating index for ${this.collection.collectionName}:`, error))
      )
    );
  }
}

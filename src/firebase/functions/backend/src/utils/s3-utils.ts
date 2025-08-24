import {
  DeleteObjectCommand,
  PutObjectCommand,
  CopyObjectCommand,
  S3Client
} from "@aws-sdk/client-s3";
import { Readable } from "stream";
import { config } from "../config";

const s3Client = new S3Client({ region: "ap-south-1" });

export async function uploadToS3({
                                   Bucket,
                                   Key,
                                   Body,
                                   ContentType,
                                 }: {
  Bucket: string;
  Key: string;
  Body: Readable | Buffer | string;
  ContentType: string;
}) {
  let contentLength;
  if (Body instanceof Readable) {
    const chunks = [];
    for await (const chunk of Body) {
      chunks.push(chunk);
    }
    Body = Buffer.concat(chunks);
    contentLength = Body.length;
  } else if (Buffer.isBuffer(Body)) {
    contentLength = Body.length;
  }

  const command = new PutObjectCommand({
    Bucket,
    Key,
    Body,
    ContentType,
    ContentLength: contentLength,
  });

  return s3Client.send(command);
}

export async function deleteFromS3({ Bucket, Key }: { Bucket: string; Key: string }) {
  const command = new DeleteObjectCommand({
    Bucket,
    Key,
  });
  return s3Client.send(command);
}

export async function copyObjectInS3({
                                       SourceBucket,
                                       SourceKey,
                                       DestinationBucket,
                                       DestinationKey,
                                     }: {
  SourceBucket: string;
  SourceKey: string;
  DestinationBucket: string;
  DestinationKey: string;
}) {
  const source = `${SourceBucket}/${SourceKey}`;
  const command = new CopyObjectCommand({
    CopySource: source,
    Bucket: DestinationBucket,
    Key: DestinationKey,
  });

  return s3Client.send(command);
}

export async function moveObjectInS3({
                                       SourceBucket,
                                       SourceKey,
                                       DestinationBucket,
                                       DestinationKey,
                                     }: {
  SourceBucket: string;
  SourceKey: string;
  DestinationBucket: string;
  DestinationKey: string;
}) {
  // Step 1: Copy the object
  await copyObjectInS3({
    SourceBucket,
    SourceKey,
    DestinationBucket,
    DestinationKey,
  });

  // Step 2: Delete the source object
  await deleteFromS3({
    Bucket: SourceBucket,
    Key: SourceKey,
  });
}

const isProduction = config.environment === "production";
const isStaging = config.environment === "staging";
const isLocal = config.environment === "local";
const envSuffix = isStaging ? "-staging" : "";

export const Buckets = {
  common: `assets${envSuffix}.xkartindia.com`,
  product: `product-assets${envSuffix}.xkartindia.com`,
  user: `user-assets${envSuffix}.xkartindia.com`,
};

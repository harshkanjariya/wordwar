import { Storage } from '@google-cloud/storage';
import { v4 as uuidv4 } from 'uuid';
import * as path from 'path';
import {config} from "../config";

const storage = new Storage({
    projectId: config.GCS_PROJECT_ID,
    credentials: {
        client_email: config.GCS_CLIENT_EMAIL,
        private_key: config.GCS_PRIVATE_KEY.replace(/\\n/g, '\n'),
    },
});

export const generatePresignedUploadUrl = async (
    bucketName: string,
    folderPath: string,
    fileName: string,
    fileType: string
) => {
    const uuid = uuidv4();
    const ext = path.extname(fileName);
    const fileKey = `${folderPath}/${uuid}${ext}`.replace(/^\/+/, '');

    const [url] = await storage.bucket(bucketName).file(fileKey).getSignedUrl({
        action: 'write',
        expires: Date.now() + 5 * 60 * 1000,
        contentType: fileType,
    });

    return { fileKey, signedUrl: url };
};

export const generatePresignedDownloadUrl = async (bucketName: string, fileKey: string) => {
    const [url] = await storage.bucket(bucketName).file(fileKey).getSignedUrl({
        action: 'read',
        expires: Date.now() + 5 * 60 * 1000,
    });
    return url;
};

export const listImages = async (bucketName: string, limit: number, pageToken?: string) => {
    const [files, meta] = await storage.bucket(bucketName).getFiles({
        maxResults: limit,
        pageToken,
    });

    const images = files
        .filter((file) => file.name.toLowerCase().match(/\.(jpg|jpeg|png|gif|bmp|webp)$/))
        .map((file) => file.name);

    return {
        images,
        nextContinuationToken: meta?.pageToken || null,
    };
};

export const deleteImage = async (bucketName: string, fileKey: string): Promise<boolean> => {
    await storage.bucket(bucketName).file(fileKey).delete();
    return true;
};

// Accepts Repositories as args to simulate DI
export const getImageUsage = async (fileKey: string) => {
    return {};
};

/**
 * Copies an object from a source location to a destination location within Google Cloud Storage.
 * This function is the GCS equivalent of the provided S3 legacy code.
 *
 * @param {object} args - The arguments for the copy operation.
 * @param {string} args.SourceBucket - The name of the bucket where the source object resides.
 * @param {string} args.SourceKey - The key (full path) of the object to be copied.
 * @param {string} args.DestinationBucket - The name of the bucket where the object will be copied to.
 * @param {string} args.DestinationKey - The key (full path) for the new object.
 * @returns {Promise<any>} A promise that resolves with the API response from the copy operation.
 */
export async function copyObject({
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
    // Get a reference to the destination file in the destination bucket
    const destinationFile = storage.bucket(DestinationBucket).file(DestinationKey);

    // Initiate the copy operation from the source file to the destination file
    const [response] = await storage
      .bucket(SourceBucket)
      .file(SourceKey)
      .copy(destinationFile);

    return response;
}

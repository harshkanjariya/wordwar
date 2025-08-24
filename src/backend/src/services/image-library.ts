import {ApiError} from "../bootstrap/errors";
import {ListObjectsV2Command, S3Client} from "@aws-sdk/client-s3";
import {GetImagesDto, GetImageUsageDto} from "../dtos/image-library";
import {repositories} from "../db/repositories";
import {Buckets, deleteFromS3} from "../utils/s3-utils";
import {FullDocument} from "../types/api";
import {User, UserType} from "../types";

const s3Client = new S3Client({region: "ap-south-1"});

const listImagesWithPagination = async (
  bucketName: string,
  limit: number,
  continuationToken?: string
) => {
  const params = {
    Bucket: bucketName,
    MaxKeys: limit,
    ContinuationToken: continuationToken,
  };

  try {
    const command = new ListObjectsV2Command(params);
    const response = await s3Client.send(command);

    const imageFiles = response.Contents?.filter((item) =>
      item.Key?.toLowerCase().match(/\.(jpg|jpeg|png|gif|bmp|webp)$/)
    ).map((item) => item.Key);

    return {
      images: imageFiles || [],
      nextContinuationToken: response.NextContinuationToken || null,
    };
  } catch (error) {
    console.error("Error fetching objects from S3:", error);
    throw error;
  }
};

export async function getImageLibrary(user: FullDocument<User>, body: GetImagesDto) {
  if (!(user.type === UserType.ADMIN || user.type === UserType.SUPER_ADMIN)) {
    throw new ApiError("Unauthorized", 401);
  }

  const {limit = 10, continuationToken = undefined} = body;

  try {
    const result = await listImagesWithPagination(body.bucket, limit, continuationToken);
    return {
      images: result.images,
      nextContinuationToken: result.nextContinuationToken,
    };
  } catch (error) {
    console.error("Error fetching images from S3:", error);
    throw new ApiError("Failed to retrieve image library", 500);
  }
}

export async function getImageUsage(user: FullDocument<User>, body: GetImageUsageDto) {
  if (!(user.type === UserType.ADMIN || user.type === UserType.SUPER_ADMIN)) {
    throw new ApiError("Unauthorized", 401);
  }

  switch (body.bucket) {
    case Buckets.user:
      const users = await repositories.users.findAll({
        filter: {
          image: body.image,
        },
        project: {
          _id: 1,
          name: 1,
        }
      });
      return {users};
  }
}

export async function deleteImages(user: FullDocument<User>, body: GetImageUsageDto) {
  if (!(user.type === UserType.ADMIN || user.type === UserType.SUPER_ADMIN)) {
    throw new ApiError("Unauthorized", 401);
  }

  const params = {
    Bucket: body.bucket,
    Key: body.image.replace(/^\/+/, ''),
  };

  try {
    return await deleteFromS3(params);
  } catch (error) {
    console.error("Error deleting images:", error);
    throw new ApiError("Failed to delete images", 500);
  }
}

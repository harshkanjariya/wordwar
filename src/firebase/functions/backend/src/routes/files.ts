import { Router } from 'express';
import {
  generatePresignedUploadUrl,
  generatePresignedDownloadUrl,
  listImages,
  deleteImage,
  getImageUsage,
} from '../services/gcs-assets';
import { catchAsync } from '../bootstrap/errors/error-handler';


export const autoPrefix = '/image-library';

const router = Router();

// Upload URL
router.get(
    '/upload-url',
    catchAsync(async (req, res) => {
      const { bucketName, folderPath, fileName, fileType } = req.query;
      const result = await generatePresignedUploadUrl(
          bucketName as string,
          folderPath as string,
          fileName as string,
          fileType as string
      );
      res.json(result);
    })
);

// Download URL
router.get(
    '/download-url',
    catchAsync(async (req, res) => {
      const { bucketName, fileKey } = req.query;
      const result = await generatePresignedDownloadUrl(bucketName as string, fileKey as string);
      res.json({ url: result });
    })
);

// List images
router.get(
    '/',
    catchAsync(async (req, res) => {
      const { bucket, limit = 20, pageToken } = req.query;
      const result = await listImages(bucket as string, Number(limit), pageToken as string);
      res.json(result);
    })
);

// Delete image
router.post(
    '/delete',
    catchAsync(async (req, res) => {
      const { bucket, image } = req.body;
      await deleteImage(bucket, image.replace(/^\/+/, ''));
      res.status(204).send();
    })
);

// Get image usage
router.get(
    '/usage',
    catchAsync(async (req, res) => {
      const { fileKey } = req.query;
      const result = await getImageUsage(fileKey as string);
      res.json(result);
    })
);

export default router;

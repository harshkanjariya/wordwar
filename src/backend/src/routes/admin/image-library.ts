import { Router, Request, Response } from "express";
import { GetImagesDto, GetImageUsageDto } from "../../dtos/image-library";
import { deleteImages, getImageLibrary, getImageUsage } from "../../services/image-library";
import { validationAndPermissionMiddleware } from "../../middlewares/validation-permission.middleware";
import { catchAsync } from "../../bootstrap/errors/error-handler";
import {PermissionValue, User} from "../../types";
import {FullDocument} from "../../types/api";

// Define the prefix for routes
export const autoPrefix = "/admin/image-library";

// Initialize Express router
const imageLibraryRoutes = Router();

// Get image library
imageLibraryRoutes.get(
  "/",
  validationAndPermissionMiddleware(GetImagesDto, PermissionValue.assets.list),
  catchAsync(async (req: Request, res: Response) => {
    const user: FullDocument<User> = req.user;
    const images = await getImageLibrary(user, req.query as any);
    res.json(images);
  })
);

// Get image usage
imageLibraryRoutes.get(
  "/usage",
  validationAndPermissionMiddleware(GetImageUsageDto, PermissionValue.assets.list),
  catchAsync(async (req: Request, res: Response) => {
    const user: FullDocument<User> = req.user;
    const usage = await getImageUsage(user, req.query as any);
    res.json(usage);
  })
);

// Delete images
imageLibraryRoutes.post(
  "/delete",
  validationAndPermissionMiddleware(GetImageUsageDto, PermissionValue.assets.delete),
  catchAsync(async (req: Request, res: Response) => {
    const user: FullDocument<User> = req.user;
    await deleteImages(user, req.body);
    res.status(204).send();
  })
);

export default imageLibraryRoutes;

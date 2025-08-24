import { Router, Request, Response } from "express";
import { getConfigurationValue } from "../services/configurations";
import { validationAndPermissionMiddleware } from "../middlewares/validation-permission.middleware";

export const autoPrefix = "/configurations";

const configurationsRoutes = Router();

configurationsRoutes.get(
  "/",
  validationAndPermissionMiddleware(null), // No DTO, no permission required
  async (_req: Request, res: Response) => {
    const result = await getConfigurationValue();
    res.json(result);
  }
);

export default configurationsRoutes;

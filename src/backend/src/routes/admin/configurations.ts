import { Router, Request, Response } from "express";
import { CreateConfigurationDto, UpdateConfigurationDto } from "../../dtos/configurations";
import {
  createConfiguration,
  deleteConfiguration,
  getConfigurationValue,
  updateConfiguration,
  upsertConfiguration,
} from "../../services/configurations";
import { validationAndPermissionMiddleware } from "../../middlewares/validation-permission.middleware";
import { catchAsync } from "../../bootstrap/errors/error-handler";
import {PermissionValue} from "../../types";

// Define the prefix for routes
export const autoPrefix = "/admin/configurations";

// Initialize Express router
const configurationRoutes = Router();

// Get all configurations
configurationRoutes.get(
  "/",
  validationAndPermissionMiddleware(null, PermissionValue.config.get),
  catchAsync(async (req: Request, res: Response) => {
    const configurations = await getConfigurationValue();
    res.json(configurations);
  })
);

// Create a new configuration
configurationRoutes.post(
  "/",
  validationAndPermissionMiddleware(CreateConfigurationDto, PermissionValue.config.create),
  catchAsync(async (req: Request, res: Response) => {
    const config = await createConfiguration(req.body);
    res.status(201).json(config);
  })
);

// Update a configuration by value
configurationRoutes.put(
  "/:value",
  validationAndPermissionMiddleware(UpdateConfigurationDto, PermissionValue.config.update),
  catchAsync(async (req: Request, res: Response) => {
    const { value } = req.params;
    const updatedConfig = await updateConfiguration(value, req.body);
    res.json(updatedConfig);
  })
);

// Upsert a configuration
configurationRoutes.post(
  "/upsert",
  validationAndPermissionMiddleware(CreateConfigurationDto, PermissionValue.config.create),
  catchAsync(async (req: Request, res: Response) => {
    const upsertedConfig = await upsertConfiguration(req.body);
    res.json(upsertedConfig);
  })
);

// Delete a configuration by value
configurationRoutes.delete(
  "/:value",
  validationAndPermissionMiddleware(null, PermissionValue.config.delete),
  catchAsync(async (req: Request, res: Response) => {
    const { value } = req.params;
    await deleteConfiguration(value);
    res.status(204).send();
  })
);

export default configurationRoutes;

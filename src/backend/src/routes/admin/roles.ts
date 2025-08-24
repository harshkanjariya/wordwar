import { Router, Request, Response } from "express";
import { CreatePermissionDto, CreateRoleDto, UpdateRoleDto } from "../../dtos/roles";
import {
  createPermission,
  createRole,
  getPermissionsList,
  getRoleDetails,
  getRoleList,
  updateRole
} from "../../services/roles";
import { validationAndPermissionMiddleware } from "../../middlewares/validation-permission.middleware";
import { catchAsync } from "../../bootstrap/errors/error-handler";
import {PermissionValue} from "../../types";

// Define the prefix for routes
export const autoPrefix = "/admin/roles";

// Initialize Express router
const roleRoutes = Router();

// Admin Role Routes

// Create Permission
roleRoutes.post(
  "/permissions",
  validationAndPermissionMiddleware(CreatePermissionDto, PermissionValue.roles.create),
  catchAsync(async (req: Request, res: Response) => {
    const permissionData: CreatePermissionDto = req.body;
    const newPermission = await createPermission(permissionData);
    res.status(201).json(newPermission);
  })
);

// Get Permissions
roleRoutes.get(
  "/permissions",
  validationAndPermissionMiddleware(null, PermissionValue.roles.get),
  catchAsync(async (req: Request, res: Response) => {
    const permissions = await getPermissionsList();
    res.json(permissions);
  })
);

// Get Roles
roleRoutes.get(
  "/",
  validationAndPermissionMiddleware(null, PermissionValue.roles.get),
  catchAsync(async (req: Request, res: Response) => {
    const roles = await getRoleList();
    res.json(roles);
  })
);

// Get Role Details
roleRoutes.get(
  "/:id",
  validationAndPermissionMiddleware(null, PermissionValue.roles.get),
  catchAsync(async (req: Request, res: Response) => {
    const { id } = req.params;
    const roleDetails = await getRoleDetails(id);
    res.json(roleDetails);
  })
);

// Create Role
roleRoutes.post(
  "/",
  validationAndPermissionMiddleware(CreateRoleDto, PermissionValue.roles.create),
  catchAsync(async (req: Request, res: Response) => {
    const roleData: CreateRoleDto = req.body;
    const newRole = await createRole(roleData);
    res.status(201).json(newRole);
  })
);

// Update Role
roleRoutes.put(
  "/:id",
  validationAndPermissionMiddleware(UpdateRoleDto, PermissionValue.roles.update),
  catchAsync(async (req: Request, res: Response) => {
    const { id } = req.params;
    const roleData: UpdateRoleDto = req.body;
    const updatedRole = await updateRole(id, roleData);
    res.json(updatedRole);
  })
);

export default roleRoutes;

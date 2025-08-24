import {Request, Response, Router} from "express";
import {SearchUserDto, UpdateProfileDto} from "../dtos/users";
import {searchUser, updateProfile,} from "../services/users/users";
import {validationAndPermissionMiddleware} from "../middlewares/validation-permission.middleware";

export const autoPrefix = "/users";

const usersRoutes = Router();

// Get user and include active shipping address and permissions
usersRoutes.get(
  "/",
  validationAndPermissionMiddleware(null),
  async (req: Request, res: Response) => {
    const user = req.user;
    res.json({
      ...user,
      permissions: req.permissions,
    });
  }
);

// Search users
usersRoutes.get(
  "/search",
  validationAndPermissionMiddleware(SearchUserDto, "user:search"),
  async (req: Request, res: Response) => {
    const body = req.query as any as SearchUserDto;  // Assuming query parameters for search
    const result = await searchUser(body, "client");
    res.json(result);
  }
);

// Update user profile
usersRoutes.put(
  "/profile",
  validationAndPermissionMiddleware(UpdateProfileDto, "user:update-profile"),
  async (req: Request, res: Response) => {
    const body = req.body as UpdateProfileDto;
    const user = req.user;
    const result = await updateProfile(body, user);
    res.json(result);
  }
);

export default usersRoutes;

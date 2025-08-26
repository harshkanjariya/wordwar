import {Request, Response, Router} from "express";
import {SearchUserDto, UpdateProfileDto} from "../dtos/users";
import {searchUser, updateProfile, getUserStatistics} from "../services/users/users";
import {validationAndPermissionMiddleware} from "../middlewares/validation-permission.middleware";
import {repositories} from "../db/repositories";

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

// Get user statistics
usersRoutes.get(
  "/statistics",
  validationAndPermissionMiddleware(null),
  async (req: Request, res: Response) => {
    const user = req.user;
    if (!user?._id) {
      return res.status(401).json({ message: "Unauthorized" });
    }
    
    try {
      const stats = await getUserStatistics(user._id.toString());
      res.json(stats);
    } catch (error) {
      console.error('Error fetching user statistics:', error);
      res.status(500).json("Failed to fetch user statistics");
    }
  }
);

// Delete user account
usersRoutes.delete(
  "/account",
  validationAndPermissionMiddleware(null),
  async (req: Request, res: Response) => {
    const user = req.user;
    if (!user?._id) {
      return res.status(401).json({ message: "Unauthorized" });
    }
    
    try {
      // Delete the user from the users collection
      await repositories.users.deleteOne({
        _id: user._id
      });
      
      // Note: We don't delete game_history or live_games data because:
      // 1. Game history only stores user IDs, which become invalid when user is deleted
      // 2. Game history objects are shared between multiple users
      // 3. Deleting shared game data would affect other players' history
      // 4. MongoDB will handle orphaned references gracefully
      
      res.json({
        status: 200,
        message: "Account deleted successfully"
      });
    } catch (error) {
      console.error('Error deleting user account:', error);
      res.status(500).json({
        status: 500,
        message: "Failed to delete account"
      });
    }
  }
);

export default usersRoutes;

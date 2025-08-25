import {NextFunction, Request, Response} from "express";
import {getRoleAllowedPermissionsCached} from "../services/roles";
import {getCachedUser} from "../services/users/users";
import jwt from "jsonwebtoken";
import {checkCurrentEmailVerified} from "../utils/user-utils";
import {User} from "../types";
import {FullDocument} from "../types/api";

export const authMiddleware = async (req: Request, res: Response, next: NextFunction) => {
  const authHeader = req.headers.authorization;

  if (authHeader && authHeader.startsWith("Bearer ")) {
    const token = authHeader.split(" ")[1];
    try {
      const decoded = jwt.decode(token) as FullDocument<User>;

      if (decoded) {
        const user = await getCachedUser(decoded._id?.toString());
        if (user) {
          req.user = user;
        }

        if (req.user?.roleId && checkCurrentEmailVerified(user)) {
          req.permissions = await getRoleAllowedPermissionsCached(req.user?.roleId?.toString());
        }
      }
    } catch (err) {
      console.warn("Invalid JWT provided", err);
    }
  }

  next();
};

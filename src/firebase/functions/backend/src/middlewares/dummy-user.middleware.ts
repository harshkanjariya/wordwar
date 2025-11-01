import {NextFunction, Request, Response} from "express";
import {isDummyUser} from "../config/dummy-user";

/**
 * Middleware to detect if the authenticated user is the dummy user
 * Adds a flag to the request object for easy checking in route handlers
 */
export const dummyUserDetectionMiddleware = async (
  req: Request,
  res: Response,
  next: NextFunction
) => {
  // Check if user is authenticated and is the dummy user
  if (req.user && req.user._id) {
    req.isDummyUser = isDummyUser(req.user._id);
  } else {
    req.isDummyUser = false;
  }

  next();
};

// Extend Express Request type to include isDummyUser flag
declare global {
  namespace Express {
    interface Request {
      isDummyUser?: boolean;
    }
  }
}

